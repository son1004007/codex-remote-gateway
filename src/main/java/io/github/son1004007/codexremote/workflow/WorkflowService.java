package io.github.son1004007.codexremote.workflow;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WorkflowService {

    private final WorkflowWorkerPort worker;
    private final Map<String, WorkflowTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, String> activeTaskIdsByWorkspace = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public WorkflowService(WorkflowWorkerPort worker) {
        this.worker = worker;
    }

    public WorkflowSnapshot create(String workspaceId, String goal, boolean autoDeploy) {
        String id = UUID.randomUUID().toString();
        if (activeTaskIdsByWorkspace.putIfAbsent(workspaceId, id) != null) {
            throw new WorkflowWorkspaceConflictException(workspaceId);
        }

        WorkflowTask task = new WorkflowTask(id, workspaceId, goal, autoDeploy);
        tasks.put(id, task);
        schedule(task);
        return task.snapshot(worker.provider());
    }

    public WorkflowSnapshot get(String workflowId) {
        return require(workflowId).snapshot(worker.provider());
    }

    public List<WorkflowSnapshot> list() {
        return tasks.values().stream()
                .map(task -> task.snapshot(worker.provider()))
                .sorted(Comparator.comparing(WorkflowSnapshot::createdAt).reversed())
                .toList();
    }

    public WorkflowSnapshot approve(String workflowId) {
        WorkflowTask task = require(workflowId);
        task.approveDeployment();
        schedule(task);
        return task.snapshot(worker.provider());
    }

    public WorkflowSnapshot resume(String workflowId) {
        WorkflowTask task = require(workflowId);
        task.resume();
        schedule(task);
        return task.snapshot(worker.provider());
    }

    public WorkflowSnapshot cancel(String workflowId) {
        WorkflowTask task = require(workflowId);
        task.cancel();
        activeTaskIdsByWorkspace.remove(task.workspaceId(), task.id());
        executor.submit(() -> safeCancel(task.id()));
        return task.snapshot(worker.provider());
    }

    private void schedule(WorkflowTask task) {
        executor.submit(() -> runUntilGate(task));
    }

    private void runUntilGate(WorkflowTask task) {
        while (true) {
            WorkflowTask.ExecutionLease lease = task.beginExecution();
            if (lease == null) {
                return;
            }

            WorkflowStage stage = lease.stage();
            try {
                WorkflowWorkerPort.Result result = worker.execute(
                        task.id(),
                        task.workspaceId(),
                        stage,
                        instruction(task, stage)
                );
                task.applyResult(stage, result);
            } catch (RuntimeException ex) {
                task.fail(stage, ex);
                return;
            }

            WorkflowStatus status = task.status();
            if (status == WorkflowStatus.COMPLETED) {
                activeTaskIdsByWorkspace.remove(task.workspaceId(), task.id());
                safeCancel(task.id());
                return;
            }
            if (status != WorkflowStatus.READY) {
                return;
            }
        }
    }

    private WorkflowTask require(String workflowId) {
        WorkflowTask task = tasks.get(workflowId);
        if (task == null) {
            throw new WorkflowNotFoundException(workflowId);
        }
        return task;
    }

    private void safeCancel(String workflowId) {
        try {
            worker.cancel(workflowId);
        } catch (RuntimeException ignored) {
            // Workflow state is already authoritative. Provider cleanup is best-effort.
        }
    }

    private static String instruction(WorkflowTask task, WorkflowStage stage) {
        return """
                You are executing one stage of a controlled software-engineering workflow.

                Workflow ID: %s
                Stage: %s
                User goal: %s

                Mandatory rules:
                - Work only inside the assigned workspace.
                - Read AGENTS.md and relevant llm-wiki documents before making decisions.
                - Preserve unrelated user changes and existing behavior.
                - Do not push, merge, or deploy unless the current stage explicitly requires it.
                - Do not invent credentials, infrastructure identifiers, test evidence, or deployment results.
                - If required information or evidence is missing, stop safely instead of guessing.

                Stage instructions:
                %s

                Finish your response with exactly one final status line:
                WORKFLOW_RESULT: SUCCESS
                or
                WORKFLOW_RESULT: BLOCKED - <reason>
                or
                WORKFLOW_RESULT: FAILED - <reason>

                SUCCESS means this stage's acceptance evidence actually exists. A plan, assumption, or intended command is not evidence of execution.
                """.formatted(task.id(), stage, task.goal(), stageInstruction(stage));
    }

    private static String stageInstruction(WorkflowStage stage) {
        return switch (stage) {
            case PLAN -> "Inspect the repository, resolve the requested scope, identify risks and dependencies, and define concrete acceptance criteria. Do not deploy.";
            case IMPLEMENT -> "Implement the planned scope with the smallest safe change. Add or update automated tests where appropriate. Do not deploy.";
            case TEST -> "Run the relevant unit/integration/static checks. Follow llm-wiki/TESTING_RULES.md when present. Fix defects that are clearly within scope and rerun affected checks. Do not deploy.";
            case REVIEW -> "Perform a skeptical review of the task changes for correctness, security, regressions, maintainability, and missing tests. Fix clear in-scope defects and rerun targeted checks. Do not deploy.";
            case DEPLOY -> "Deployment has passed the workflow approval gate. Use only repository-defined deployment procedures and only the target explicitly available in repository configuration or the user goal. If the target or required credentials are missing, return BLOCKED. Never infer a production target.";
            case E2E -> "Run the repository-defined smoke/E2E verification against the deployed target. Record actual evidence and fail or block if the environment cannot prove the acceptance criteria.";
        };
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}

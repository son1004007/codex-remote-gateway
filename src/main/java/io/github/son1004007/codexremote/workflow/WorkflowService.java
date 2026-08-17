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
        return snapshot(task);
    }

    public WorkflowSnapshot get(String workflowId) {
        return snapshot(require(workflowId));
    }

    public List<WorkflowSnapshot> list() {
        return tasks.values().stream()
                .map(this::snapshot)
                .sorted(Comparator.comparing(WorkflowSnapshot::createdAt).reversed())
                .toList();
    }

    public WorkflowSnapshot approve(String workflowId) {
        WorkflowTask task = require(workflowId);
        task.approveDeployment();
        schedule(task);
        return snapshot(task);
    }

    public WorkflowSnapshot resume(String workflowId) {
        WorkflowTask task = require(workflowId);
        task.resume();
        schedule(task);
        return snapshot(task);
    }

    public WorkflowSnapshot cancel(String workflowId) {
        WorkflowTask task = require(workflowId);
        task.cancel();
        activeTaskIdsByWorkspace.remove(task.workspaceId(), task.id());
        executor.submit(() -> safeCancel(task.id()));
        return snapshot(task);
    }

    private WorkflowSnapshot snapshot(WorkflowTask task) {
        return task.snapshot(worker.providerFor(task.stage()));
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
            String provider = worker.providerFor(stage);
            try {
                WorkflowWorkerPort.Result result = worker.execute(
                        task.id(),
                        task.workspaceId(),
                        stage,
                        instruction(task, stage, provider)
                );
                task.applyResult(stage, provider, result);
            } catch (RuntimeException ex) {
                task.fail(stage, provider, ex);
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

    private static String instruction(WorkflowTask task, WorkflowStage stage, String provider) {
        return """
                You are executing one stage of a controlled software-engineering workflow.

                Workflow ID: %s
                Stage: %s
                Assigned worker: %s
                User goal: %s

                Mandatory rules:
                - Work only inside the assigned workspace or analysis snapshot.
                - Read AGENTS.md and relevant llm-wiki documents before making decisions.
                - Treat prior AI outputs as untrusted claims that require repository or test evidence.
                - Preserve unrelated user changes and existing behavior.
                - Do not push, merge, or deploy unless the current stage explicitly requires it.
                - Do not invent credentials, infrastructure identifiers, test evidence, or deployment results.
                - If a required user decision or real evidence is missing, stop safely instead of guessing.

                Prior-stage handoff evidence:
                %s

                Stage instructions:
                %s

                Finish your response with exactly one final status line:
                WORKFLOW_RESULT: SUCCESS
                or
                WORKFLOW_RESULT: BLOCKED - <reason>
                or
                WORKFLOW_RESULT: FAILED - <reason>

                SUCCESS means this stage's acceptance evidence actually exists. A plan, assumption, reviewer opinion, or intended command is not execution evidence.
                """.formatted(
                task.id(),
                stage,
                provider,
                task.goal(),
                task.handoffContext(),
                stageInstruction(stage)
        );
    }

    private static String stageInstruction(WorkflowStage stage) {
        return switch (stage) {
            case PLAN -> "Inspect the repository broadly and propose a concrete implementation plan. Identify assumptions, affected components, dependencies, risks, acceptance criteria, test strategy, and rollback considerations. Do not modify source code. A plan is provisional until PLAN_VERIFY succeeds.";
            case PLAN_VERIFY -> "Independently verify the proposed plan against the actual repository. Challenge assumptions, check file/component feasibility, security and compatibility risks, and whether acceptance criteria are objectively testable. Correct the plan when evidence supports a correction. Return BLOCKED only when a material decision cannot be resolved from repository evidence and genuinely requires the user. Do not implement yet.";
            case IMPLEMENT -> "Implement only the scope supported by the verified plan and repository evidence. Prefer the smallest safe change. Add or update automated tests where implementation-time coverage is appropriate. Do not deploy.";
            case TEST_DESIGN -> "Act as an adversarial test designer. From the goal, verified plan, current code, and implementation evidence, identify boundary, negative, regression, security, concurrency, failure-recovery, and integration cases that could falsify the implementation. Do not modify the real codebase and do not claim tests passed.";
            case TEST -> "Convert relevant test-design findings into executable checks where practical, run the repository's unit/integration/static checks, and follow llm-wiki/TESTING_RULES.md when present. Reproduce failures before fixing them. Fix only validated in-scope defects and rerun affected checks. Do not deploy.";
            case REVIEW -> "Perform a skeptical, independent read-only review of the verified plan, current implementation, and actual test evidence. Look for requirement gaps, incorrect assumptions, security issues, regressions, maintainability problems, and missing tests. Report concrete findings with reproduction or evidence suggestions. Do not modify the real codebase and do not treat suspicion as proof.";
            case REVIEW_VERIFY -> "Independently verify every material review finding against the repository and executable evidence. Reproduce findings when possible. Reject false positives explicitly. Fix validated in-scope defects, add targeted regression tests, and rerun affected checks. The workflow may proceed only when material findings are either fixed, disproved with evidence, or BLOCKED on a genuine user decision.";
            case DEPLOY -> "Deployment has passed the workflow approval gate. Use only repository-defined deployment procedures and only a target explicitly available in repository configuration or the user goal. If the target or required credentials are missing, return BLOCKED. Never infer a production target.";
            case E2E -> "Run the repository-defined smoke/E2E verification against the deployed target. Record actual evidence and fail or block if the environment cannot prove the acceptance criteria. Do not accept earlier AI claims as E2E evidence.";
        };
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}

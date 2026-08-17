package io.github.son1004007.codexremote.workflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class WorkflowTask {

    private static final int MAX_HANDOFF_STAGE_CHARS = 1_500;
    private static final int MAX_HANDOFF_TOTAL_CHARS = 12_000;

    private final String id;
    private final String workspaceId;
    private final String goal;
    private final boolean autoDeploy;
    private final Instant createdAt;
    private final List<WorkflowEvent> events = new ArrayList<>();
    private final EnumMap<WorkflowStage, String> stageWorkers = new EnumMap<>(WorkflowStage.class);
    private final EnumMap<WorkflowStage, String> stageOutputs = new EnumMap<>(WorkflowStage.class);

    private WorkflowStatus status = WorkflowStatus.READY;
    private WorkflowStage stage = WorkflowStage.PLAN;
    private String workerSessionId;
    private boolean deployApproved;
    private String lastOutput;
    private String lastError;
    private Instant updatedAt;

    WorkflowTask(String id, String workspaceId, String goal, boolean autoDeploy) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.goal = goal;
        this.autoDeploy = autoDeploy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.events.add(WorkflowEvent.of("WORKFLOW_CREATED", stage, "Workflow created"));
    }

    synchronized ExecutionLease beginExecution() {
        if (status == WorkflowStatus.CANCELLED || status == WorkflowStatus.COMPLETED) {
            return null;
        }
        if (status == WorkflowStatus.RUNNING) {
            return null;
        }
        if (stage == WorkflowStage.DEPLOY && !autoDeploy && !deployApproved) {
            status = WorkflowStatus.WAITING_APPROVAL;
            updatedAt = Instant.now();
            events.add(WorkflowEvent.of("APPROVAL_REQUIRED", stage, "Deployment approval required"));
            return null;
        }
        if (status != WorkflowStatus.READY) {
            return null;
        }
        status = WorkflowStatus.RUNNING;
        updatedAt = Instant.now();
        events.add(WorkflowEvent.of("STAGE_STARTED", stage, "Stage started"));
        return new ExecutionLease(stage);
    }

    synchronized void applyResult(
            WorkflowStage executedStage,
            String provider,
            WorkflowWorkerPort.Result result
    ) {
        if (status == WorkflowStatus.CANCELLED) {
            return;
        }
        if (stage != executedStage) {
            throw new WorkflowStateException("Workflow stage changed while worker was running");
        }

        this.workerSessionId = result.sessionId();
        this.lastOutput = truncate(result.message());
        this.updatedAt = Instant.now();
        this.stageWorkers.put(executedStage, provider);
        this.stageOutputs.put(executedStage, lastOutput);

        switch (result.outcome()) {
            case SUCCESS -> {
                events.add(WorkflowEvent.of("STAGE_SUCCEEDED", stage, provider + ": " + lastOutput));
                lastError = null;
                if (stage == WorkflowStage.E2E) {
                    status = WorkflowStatus.COMPLETED;
                    events.add(WorkflowEvent.of("WORKFLOW_COMPLETED", stage, "Workflow completed"));
                } else {
                    stage = stage.next();
                    status = WorkflowStatus.READY;
                }
            }
            case BLOCKED -> {
                status = WorkflowStatus.BLOCKED;
                lastError = lastOutput;
                events.add(WorkflowEvent.of("STAGE_BLOCKED", stage, provider + ": " + lastOutput));
            }
            case FAILED -> {
                status = WorkflowStatus.FAILED;
                lastError = lastOutput;
                events.add(WorkflowEvent.of("STAGE_FAILED", stage, provider + ": " + lastOutput));
            }
        }
    }

    synchronized void fail(WorkflowStage failedStage, String provider, RuntimeException ex) {
        if (status == WorkflowStatus.CANCELLED) {
            return;
        }
        status = WorkflowStatus.FAILED;
        lastError = truncate(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        updatedAt = Instant.now();
        stageWorkers.put(failedStage, provider);
        events.add(WorkflowEvent.of("WORKER_ERROR", failedStage, provider + ": " + lastError));
    }

    synchronized void approveDeployment() {
        if (status != WorkflowStatus.WAITING_APPROVAL || stage != WorkflowStage.DEPLOY) {
            throw new WorkflowStateException("Workflow is not waiting for deployment approval");
        }
        deployApproved = true;
        status = WorkflowStatus.READY;
        updatedAt = Instant.now();
        events.add(WorkflowEvent.of("DEPLOY_APPROVED", stage, "Deployment approved"));
    }

    synchronized void resume() {
        if (status != WorkflowStatus.BLOCKED && status != WorkflowStatus.FAILED) {
            throw new WorkflowStateException("Only blocked or failed workflows can be resumed");
        }
        status = WorkflowStatus.READY;
        lastError = null;
        updatedAt = Instant.now();
        events.add(WorkflowEvent.of("WORKFLOW_RESUMED", stage, "Workflow resumed"));
    }

    synchronized void cancel() {
        if (status == WorkflowStatus.CANCELLED) {
            return;
        }
        if (status == WorkflowStatus.COMPLETED) {
            throw new WorkflowStateException("Completed workflow cannot be cancelled");
        }
        status = WorkflowStatus.CANCELLED;
        updatedAt = Instant.now();
        events.add(WorkflowEvent.of("WORKFLOW_CANCELLED", stage, "Workflow cancelled"));
    }

    synchronized WorkflowSnapshot snapshot(String currentWorker) {
        return new WorkflowSnapshot(
                id,
                workspaceId,
                goal,
                status,
                stage,
                currentWorker,
                workerSessionId,
                autoDeploy,
                lastOutput,
                lastError,
                Map.copyOf(stageWorkers),
                Map.copyOf(stageOutputs),
                createdAt,
                updatedAt,
                List.copyOf(events)
        );
    }

    synchronized String handoffContext() {
        if (stageOutputs.isEmpty()) {
            return "No prior stage evidence.";
        }

        StringBuilder context = new StringBuilder();
        for (WorkflowStage candidate : WorkflowStage.values()) {
            String output = stageOutputs.get(candidate);
            if (output == null) {
                continue;
            }
            String worker = stageWorkers.getOrDefault(candidate, "unknown");
            String snippet = output.length() <= MAX_HANDOFF_STAGE_CHARS
                    ? output
                    : output.substring(0, MAX_HANDOFF_STAGE_CHARS) + "...[truncated]";
            String block = "\n--- " + candidate + " by " + worker + " ---\n" + snippet + "\n";
            if (context.length() + block.length() > MAX_HANDOFF_TOTAL_CHARS) {
                context.append("\n...[prior-stage context truncated]\n");
                break;
            }
            context.append(block);
        }
        return context.toString();
    }

    synchronized WorkflowStatus status() {
        return status;
    }

    synchronized WorkflowStage stage() {
        return stage;
    }

    String id() {
        return id;
    }

    String workspaceId() {
        return workspaceId;
    }

    String goal() {
        return goal;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.length() <= 4000 ? normalized : normalized.substring(0, 4000) + "...";
    }

    record ExecutionLease(WorkflowStage stage) {
    }
}

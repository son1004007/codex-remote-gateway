package io.github.son1004007.codexremote.workflow;

public interface WorkflowWorkerPort {

    String provider();

    default String providerFor(WorkflowStage stage) {
        return provider();
    }

    Result execute(String workflowId, String workspaceId, WorkflowStage stage, String instruction);

    void cancel(String workflowId);

    enum Outcome {
        SUCCESS,
        BLOCKED,
        FAILED
    }

    record Result(String sessionId, Outcome outcome, String message) {
    }
}

package io.github.son1004007.codexremote.workflow;

interface WorkflowStageWorker {

    String provider();

    WorkflowWorkerPort.Result execute(
            String workflowId,
            String workspaceId,
            WorkflowStage stage,
            String instruction
    );

    void cancel(String workflowId);
}

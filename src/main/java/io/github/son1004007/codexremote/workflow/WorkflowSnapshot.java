package io.github.son1004007.codexremote.workflow;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record WorkflowSnapshot(
        String id,
        String workspaceId,
        String goal,
        WorkflowStatus status,
        WorkflowStage stage,
        String worker,
        String workerSessionId,
        boolean autoDeploy,
        String lastOutput,
        String lastError,
        Map<WorkflowStage, String> stageWorkers,
        Map<WorkflowStage, String> stageOutputs,
        Instant createdAt,
        Instant updatedAt,
        List<WorkflowEvent> events
) {
}

package io.github.son1004007.codexremote.workflow;

import java.time.Instant;
import java.util.List;

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
        Instant createdAt,
        Instant updatedAt,
        List<WorkflowEvent> events
) {
}

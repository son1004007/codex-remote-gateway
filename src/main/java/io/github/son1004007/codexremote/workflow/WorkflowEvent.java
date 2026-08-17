package io.github.son1004007.codexremote.workflow;

import java.time.Instant;

public record WorkflowEvent(
        String type,
        WorkflowStage stage,
        String message,
        Instant occurredAt
) {
    public static WorkflowEvent of(String type, WorkflowStage stage, String message) {
        return new WorkflowEvent(type, stage, message, Instant.now());
    }
}

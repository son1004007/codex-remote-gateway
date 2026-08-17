package io.github.son1004007.codexremote.workflow;

final class WorkflowNotFoundException extends RuntimeException {
    WorkflowNotFoundException(String workflowId) {
        super("Workflow not found: " + workflowId);
    }
}

final class WorkflowStateException extends RuntimeException {
    WorkflowStateException(String message) {
        super(message);
    }
}

final class WorkflowWorkspaceConflictException extends RuntimeException {
    WorkflowWorkspaceConflictException(String workspaceId) {
        super("An active workflow already exists for workspace: " + workspaceId);
    }
}

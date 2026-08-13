package io.github.son1004007.codexremote.session;

public class WorkspaceSessionConflictException extends IllegalStateException {

    public WorkspaceSessionConflictException(String workspaceId) {
        super("Workspace already has an active session: " + workspaceId);
    }
}

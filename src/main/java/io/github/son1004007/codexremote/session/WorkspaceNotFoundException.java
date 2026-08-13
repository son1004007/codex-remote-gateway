package io.github.son1004007.codexremote.session;

public class WorkspaceNotFoundException extends RuntimeException {

    public WorkspaceNotFoundException(String workspaceId) {
        super("Workspace not found: " + workspaceId);
    }
}

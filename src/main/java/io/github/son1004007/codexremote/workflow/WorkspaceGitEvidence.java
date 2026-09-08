package io.github.son1004007.codexremote.workflow;

import io.github.son1004007.codexremote.workspace.WorkspaceService;

public record WorkspaceGitEvidence(
        boolean available,
        WorkspaceService.GitResult status,
        WorkspaceService.GitResult unstagedDiff,
        WorkspaceService.GitResult stagedDiff,
        String error
) {
    static WorkspaceGitEvidence unavailable(String error) {
        return new WorkspaceGitEvidence(false, null, null, null, error);
    }
}

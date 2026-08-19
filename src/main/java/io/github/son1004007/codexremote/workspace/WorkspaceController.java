package io.github.son1004007.codexremote.workspace;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@ConditionalOnProperty(name = "gateway.agent.mode", havingValue = "codex")
public class WorkspaceController {

    private final WorkspaceService workspaces;

    public WorkspaceController(WorkspaceService workspaces) {
        this.workspaces = workspaces;
    }

    @GetMapping
    public List<WorkspaceService.WorkspaceSummary> list() {
        return workspaces.list();
    }

    @GetMapping("/{workspaceId}/git/status")
    public WorkspaceService.GitResult gitStatus(@PathVariable String workspaceId) {
        return workspaces.gitStatus(workspaceId);
    }

    @GetMapping("/{workspaceId}/git/diff")
    public WorkspaceService.GitResult gitDiff(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "false") boolean staged
    ) {
        return workspaces.gitDiff(workspaceId, staged);
    }
}

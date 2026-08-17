package io.github.son1004007.codexremote.workflow;

import io.github.son1004007.codexremote.config.GatewayProperties;
import io.github.son1004007.codexremote.session.WorkspaceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "gateway.agent.mode", havingValue = "codex")
class WorkflowWorkspaceResolver {

    private final Path workspaceRoot;

    WorkflowWorkspaceResolver(GatewayProperties properties) {
        Path configuredRoot = Path.of(properties.getCodex().getWorkspaceRoot()).toAbsolutePath().normalize();
        try {
            this.workspaceRoot = configuredRoot.toRealPath();
        } catch (IOException ex) {
            throw new IllegalStateException("Configured workspace root is not accessible: " + configuredRoot, ex);
        }
    }

    Path resolve(String workspaceId) {
        if (workspaceId == null
                || workspaceId.isBlank()
                || ".".equals(workspaceId)
                || "..".equals(workspaceId)
                || workspaceId.indexOf('\0') >= 0
                || workspaceId.contains("/")
                || workspaceId.contains("\\")) {
            throw new WorkspaceNotFoundException(workspaceId);
        }

        try {
            Path candidate = workspaceRoot.resolve(workspaceId);
            if (!Files.isDirectory(candidate)) {
                throw new WorkspaceNotFoundException(workspaceId);
            }
            Path realCandidate = candidate.toRealPath();
            if (!workspaceRoot.equals(realCandidate.getParent())) {
                throw new WorkspaceNotFoundException(workspaceId);
            }
            return realCandidate;
        } catch (IOException | InvalidPathException ex) {
            throw new WorkspaceNotFoundException(workspaceId);
        }
    }
}

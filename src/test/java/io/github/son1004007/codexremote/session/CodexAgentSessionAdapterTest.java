package io.github.son1004007.codexremote.session;

import io.github.son1004007.codexremote.config.GatewayProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CodexAgentSessionAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsDirectoryInsideWorkspaceRoot() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("workspaces"));
        Files.createDirectory(root.resolve("project-a"));
        CodexAgentSessionAdapter adapter = adapter(root);

        assertThatCode(() -> adapter.create("project-a")).doesNotThrowAnyException();
    }

    @Test
    void rejectsParentTraversal() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("workspaces"));
        Files.createDirectory(tempDir.resolve("outside"));
        CodexAgentSessionAdapter adapter = adapter(root);

        assertThatThrownBy(() -> adapter.create("../outside"))
                .isInstanceOf(WorkspaceNotFoundException.class);
    }

    @Test
    void rejectsAbsolutePathInjection() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("workspaces"));
        Path outside = Files.createDirectory(tempDir.resolve("absolute-outside"));
        CodexAgentSessionAdapter adapter = adapter(root);

        assertThatThrownBy(() -> adapter.create(outside.toAbsolutePath().toString()))
                .isInstanceOf(WorkspaceNotFoundException.class);
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void rejectsSymlinkEscape() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("workspaces"));
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Files.createSymbolicLink(root.resolve("escape"), outside);
        CodexAgentSessionAdapter adapter = adapter(root);

        assertThatThrownBy(() -> adapter.create("escape"))
                .isInstanceOf(WorkspaceNotFoundException.class);
    }

    private CodexAgentSessionAdapter adapter(Path root) {
        GatewayProperties properties = new GatewayProperties();
        properties.getCodex().setWorkspaceRoot(root.toString());
        return new CodexAgentSessionAdapter(mock(CodexAppServerClient.class), properties);
    }
}

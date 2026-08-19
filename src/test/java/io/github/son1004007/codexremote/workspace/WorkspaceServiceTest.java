package io.github.son1004007.codexremote.workspace;

import io.github.son1004007.codexremote.config.GatewayProperties;
import io.github.son1004007.codexremote.session.WorkspaceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void listsOnlyDirectDirectoryWorkspaces() throws Exception {
        Files.createDirectories(tempDir.resolve("alpha"));
        Files.createDirectories(tempDir.resolve("beta"));
        Files.writeString(tempDir.resolve("not-a-workspace.txt"), "x");

        WorkspaceService service = service();

        assertThat(service.list())
                .extracting(WorkspaceService.WorkspaceSummary::id)
                .containsExactly("alpha", "beta");
    }

    @Test
    void rejectsTraversalAndNestedWorkspaceIds() throws Exception {
        Files.createDirectories(tempDir.resolve("repo").resolve("nested"));
        WorkspaceService service = service();

        assertThatThrownBy(() -> service.resolve("../repo"))
                .isInstanceOf(WorkspaceNotFoundException.class);
        assertThatThrownBy(() -> service.resolve("repo/nested"))
                .isInstanceOf(WorkspaceNotFoundException.class);
    }

    @Test
    void returnsBoundedGitStatusForRepository() throws Exception {
        Path repo = Files.createDirectories(tempDir.resolve("repo"));
        run(repo, "git", "init", "-q");
        Files.writeString(repo.resolve("new-file.txt"), "hello\n");

        WorkspaceService service = service();
        WorkspaceService.GitResult result = service.gitStatus("repo");

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains("?? new-file.txt");
        assertThat(result.truncated()).isFalse();
    }

    private WorkspaceService service() {
        GatewayProperties properties = new GatewayProperties();
        properties.getCodex().setWorkspaceRoot(tempDir.toString());
        return new WorkspaceService(properties);
    }

    private static void run(Path directory, String... command) throws Exception {
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", command) + "\n" + output);
        }
    }
}

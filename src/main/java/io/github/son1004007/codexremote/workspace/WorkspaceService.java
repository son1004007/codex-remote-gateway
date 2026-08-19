package io.github.son1004007.codexremote.workspace;

import io.github.son1004007.codexremote.config.GatewayProperties;
import io.github.son1004007.codexremote.session.WorkspaceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class WorkspaceService {

    private static final int MAX_GIT_OUTPUT_BYTES = 512 * 1024;
    private static final Duration GIT_TIMEOUT = Duration.ofSeconds(10);

    private final Path workspaceRoot;

    public WorkspaceService(GatewayProperties properties) {
        Path configuredRoot = Path.of(properties.getCodex().getWorkspaceRoot()).toAbsolutePath().normalize();
        try {
            this.workspaceRoot = configuredRoot.toRealPath();
        } catch (IOException ex) {
            throw new IllegalStateException("Configured workspace root is not accessible: " + configuredRoot, ex);
        }
    }

    public List<WorkspaceSummary> list() {
        try (var children = Files.list(workspaceRoot)) {
            return children
                    .filter(path -> Files.isDirectory(path) && !Files.isSymbolicLink(path))
                    .map(path -> new WorkspaceSummary(
                            path.getFileName().toString(),
                            Files.isDirectory(path.resolve(".git"))))
                    .sorted(Comparator.comparing(WorkspaceSummary::id))
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to enumerate workspaces", ex);
        }
    }

    public GitResult gitStatus(String workspaceId) {
        Path workspace = resolve(workspaceId);
        ensureGitRepository(workspace, workspaceId);
        return runGit(workspace, List.of("status", "--short", "--branch"));
    }

    public GitResult gitDiff(String workspaceId, boolean staged) {
        Path workspace = resolve(workspaceId);
        ensureGitRepository(workspace, workspaceId);
        if (staged) {
            return runGit(workspace, List.of("diff", "--cached", "--no-ext-diff", "--no-color", "--", "."));
        }
        return runGit(workspace, List.of("diff", "--no-ext-diff", "--no-color", "--", "."));
    }

    public Path resolve(String workspaceId) {
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
            if (!Files.isDirectory(candidate) || Files.isSymbolicLink(candidate)) {
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

    private void ensureGitRepository(Path workspace, String workspaceId) {
        if (!Files.isDirectory(workspace.resolve(".git"))) {
            throw new IllegalStateException("Workspace is not a Git repository: " + workspaceId);
        }
    }

    private GitResult runGit(Path workspace, List<String> arguments) {
        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("codex-git-", ".log");
            var command = new java.util.ArrayList<String>();
            command.add("git");
            command.addAll(arguments);

            Process process = new ProcessBuilder(command)
                    .directory(workspace.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(outputFile.toFile())
                    .start();

            boolean completed = process.waitFor(GIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                throw new IllegalStateException("Git command timed out");
            }

            byte[] bytes = Files.readAllBytes(outputFile);
            boolean truncated = bytes.length > MAX_GIT_OUTPUT_BYTES;
            int length = Math.min(bytes.length, MAX_GIT_OUTPUT_BYTES);
            String output = new String(bytes, 0, length, StandardCharsets.UTF_8);
            if (truncated) {
                output += "\n...[output truncated by gateway]";
            }
            return new GitResult(process.exitValue(), output, truncated);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to execute Git command", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Git command interrupted", ex);
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException ignored) {
                    // Best-effort cleanup only.
                }
            }
        }
    }

    public record WorkspaceSummary(String id, boolean gitRepository) {
    }

    public record GitResult(int exitCode, String output, boolean truncated) {
    }
}

package io.github.son1004007.codexremote.workflow;

import io.github.son1004007.codexremote.workspace.WorkspaceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "gateway.agent.mode", havingValue = "codex")
public class WorkflowVerificationService {

    private static final int MAX_OUTPUT_BYTES = 512 * 1024;
    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private final WorkspaceService workspaces;

    public WorkflowVerificationService(WorkspaceService workspaces) {
        this.workspaces = workspaces;
    }

    public WorkflowVerificationResult verify(String workspaceId, VerificationProfile profile) {
        Path workspace = workspaces.resolve(workspaceId);
        List<String> command = commandFor(workspace, profile);
        Instant startedAt = Instant.now();
        Path outputFile = null;

        try {
            outputFile = Files.createTempFile("workflow-verification-", ".log");
            Process process = new ProcessBuilder(command)
                    .directory(workspace.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(outputFile.toFile())
                    .start();

            boolean completed = process.waitFor(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                return result(profile, command, -1, false,
                        "Verification timed out after " + TIMEOUT, false, startedAt);
            }

            Output output = readOutput(outputFile);
            int exitCode = process.exitValue();
            return result(profile, command, exitCode, exitCode == 0,
                    output.text(), output.truncated(), startedAt);
        } catch (IOException ex) {
            return result(profile, command, -1, false,
                    "Failed to start verification: " + ex.getMessage(), false, startedAt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return result(profile, command, -1, false,
                    "Verification interrupted", false, startedAt);
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException ignored) {
                    // Best-effort cleanup.
                }
            }
        }
    }

    private static List<String> commandFor(Path workspace, VerificationProfile profile) {
        return switch (profile) {
            case MAVEN_TEST -> executableWrapperOr(workspace, "mvnw",
                    List.of("./mvnw", "-q", "test"), List.of("mvn", "-q", "test"));
            case MAVEN_VERIFY -> executableWrapperOr(workspace, "mvnw",
                    List.of("./mvnw", "-q", "verify"), List.of("mvn", "-q", "verify"));
            case GRADLE_TEST -> executableWrapperOr(workspace, "gradlew",
                    List.of("./gradlew", "test", "--no-daemon"), List.of("gradle", "test", "--no-daemon"));
            case PYTEST -> List.of("python3", "-m", "pytest", "-q");
            case NPM_TEST -> List.of("npm", "test");
            case GIT_DIFF_CHECK -> List.of("git", "diff", "--check");
        };
    }

    private static List<String> executableWrapperOr(
            Path workspace,
            String wrapperName,
            List<String> wrapperCommand,
            List<String> fallbackCommand
    ) {
        Path wrapper = workspace.resolve(wrapperName);
        return Files.isRegularFile(wrapper) && Files.isExecutable(wrapper)
                ? wrapperCommand
                : fallbackCommand;
    }

    private static Output readOutput(Path outputFile) throws IOException {
        byte[] bytes;
        try (InputStream input = Files.newInputStream(outputFile)) {
            bytes = input.readNBytes(MAX_OUTPUT_BYTES + 1);
        }
        boolean truncated = bytes.length > MAX_OUTPUT_BYTES;
        int length = Math.min(bytes.length, MAX_OUTPUT_BYTES);
        String output = new String(bytes, 0, length, StandardCharsets.UTF_8);
        if (truncated) {
            output += "\n...[verification output truncated by gateway]";
        }
        return new Output(output, truncated);
    }

    private static WorkflowVerificationResult result(
            VerificationProfile profile,
            List<String> command,
            int exitCode,
            boolean success,
            String output,
            boolean truncated,
            Instant startedAt
    ) {
        return new WorkflowVerificationResult(
                profile,
                List.copyOf(command),
                exitCode,
                success,
                output == null ? "" : output,
                truncated,
                startedAt,
                Instant.now()
        );
    }

    private record Output(String text, boolean truncated) {
    }
}

package io.github.son1004007.codexremote.workflow;

import io.github.son1004007.codexremote.config.GatewayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "gateway.antigravity.enabled", havingValue = "true")
class AntigravityWorkflowWorkerAdapter implements WorkflowStageWorker {

    private static final int MAX_GIT_CONTEXT = 20_000;
    private static final Set<String> SKIPPED_DIRECTORIES = Set.of(
            ".git", "target", "build", "node_modules", ".gradle", ".idea",
            ".venv", "venv", "__pycache__"
    );

    private final WorkflowWorkspaceResolver workspaceResolver;
    private final GatewayProperties.Antigravity properties;
    private final ConcurrentHashMap<String, Process> activeProcesses = new ConcurrentHashMap<>();

    AntigravityWorkflowWorkerAdapter(
            WorkflowWorkspaceResolver workspaceResolver,
            GatewayProperties properties
    ) {
        this.workspaceResolver = workspaceResolver;
        this.properties = properties.getAntigravity();
    }

    @Override
    public String provider() {
        return properties.getModel().isBlank()
                ? "antigravity"
                : "antigravity:" + properties.getModel();
    }

    @Override
    public WorkflowWorkerPort.Result execute(
            String workflowId,
            String workspaceId,
            WorkflowStage stage,
            String instruction
    ) {
        Path source = workspaceResolver.resolve(workspaceId);
        Path snapshot = null;
        Process process = null;
        try {
            snapshot = Files.createTempDirectory("codex-remote-agy-");
            copyAnalysisSnapshot(source, snapshot);

            String prompt = instruction
                    + "\n\nAntigravity safety boundary:\n"
                    + "- This directory is a disposable analysis snapshot, not the real workspace.\n"
                    + "- Do not edit files, create files, run deployment commands, or attempt Git writes.\n"
                    + "- Analyze only. Return findings and the required WORKFLOW_RESULT marker.\n\n"
                    + gitContext(source);

            List<String> command = new ArrayList<>();
            command.add(properties.getCommand());
            command.add("--prompt");
            command.add(prompt);
            command.add("--sandbox");
            command.add("--print-timeout");
            command.add(durationArgument(properties.getTurnTimeout()));
            if (!properties.getModel().isBlank()) {
                command.add("--model");
                command.add(properties.getModel());
            }

            process = new ProcessBuilder(command)
                    .directory(snapshot.toFile())
                    .redirectErrorStream(true)
                    .start();
            activeProcesses.put(workflowId, process);

            boolean finished = process.waitFor(
                    properties.getTurnTimeout().plusSeconds(15).toMillis(),
                    TimeUnit.MILLISECONDS
            );
            if (!finished) {
                process.destroyForcibly();
                return new WorkflowWorkerPort.Result(
                        "antigravity-" + workflowId + "-" + stage,
                        WorkflowWorkerPort.Outcome.FAILED,
                        "Antigravity analysis timed out\nWORKFLOW_RESULT: FAILED - analysis timeout"
                );
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                return new WorkflowWorkerPort.Result(
                        "antigravity-" + workflowId + "-" + stage,
                        WorkflowWorkerPort.Outcome.FAILED,
                        output + "\nWORKFLOW_RESULT: FAILED - Antigravity CLI exited with code " + process.exitValue()
                );
            }
            return new WorkflowWorkerPort.Result(
                    "antigravity-" + workflowId + "-" + stage,
                    WorkflowResultParser.parse(output),
                    output
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start Antigravity CLI", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Antigravity CLI", ex);
        } finally {
            if (process != null) {
                activeProcesses.remove(workflowId, process);
            }
            deleteRecursively(snapshot);
        }
    }

    @Override
    public void cancel(String workflowId) {
        Process process = activeProcesses.remove(workflowId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }

    private static String durationArgument(Duration duration) {
        long seconds = Math.max(1, duration.toSeconds());
        return seconds + "s";
    }

    private static String gitContext(Path workspace) {
        String status = runReadOnlyGit(workspace, List.of("git", "status", "--short"));
        String diff = runReadOnlyGit(workspace, List.of("git", "diff", "--no-ext-diff", "--unified=3"));
        String combined = "Git status:\n" + status + "\n\nCurrent working-tree diff:\n" + diff;
        if (combined.length() <= MAX_GIT_CONTEXT) {
            return combined;
        }
        return combined.substring(0, MAX_GIT_CONTEXT) + "\n...[git context truncated]";
    }

    private static String runReadOnlyGit(Path workspace, List<String> command) {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(workspace.toFile())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "[git context unavailable: timeout]";
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return process.exitValue() == 0 ? output : "[git context unavailable]";
        } catch (IOException ex) {
            return "[git context unavailable]";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "[git context unavailable: interrupted]";
        }
    }

    private static void copyAnalysisSnapshot(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(source) && SKIPPED_DIRECTORIES.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path relative = source.relativize(dir);
                Files.createDirectories(target.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isSymbolicLink(file)) {
                    return FileVisitResult.CONTINUE;
                }
                Path relative = source.relativize(file);
                Files.copy(file, target.resolve(relative));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // Temporary analysis snapshots are best-effort cleanup only.
        }
    }
}

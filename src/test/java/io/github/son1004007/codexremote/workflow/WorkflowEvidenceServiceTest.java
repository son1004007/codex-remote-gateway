package io.github.son1004007.codexremote.workflow;

import io.github.son1004007.codexremote.config.GatewayProperties;
import io.github.son1004007.codexremote.workspace.WorkspaceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowEvidenceServiceTest {

    @TempDir
    Path tempDir;

    private final WorkflowService workflows = new WorkflowService(new InMemoryWorkflowWorkerAdapter());

    @AfterEach
    void tearDown() {
        workflows.shutdown();
    }

    @Test
    void keepsModelStageEvidenceSeparateFromDeterministicVerificationAndGitEvidence() throws Exception {
        Path workspace = createGitWorkspace("demo");
        Files.writeString(workspace.resolve("new-file.txt"), "evidence\n");

        WorkflowSnapshot created = workflows.create(
                "demo",
                "Implement a verified workflow feature",
                List.of("Repository checks pass", "Evidence bundle exposes Git state"),
                false
        );
        awaitStatus(created.id(), WorkflowStatus.WAITING_APPROVAL);

        WorkspaceService workspaceService = workspaceService();
        WorkflowEvidenceService service = new WorkflowEvidenceService(
                workflows,
                workspaceService,
                new WorkflowVerificationService(workspaceService)
        );

        WorkflowVerificationResult verification = service.verify(
                created.id(), VerificationProfile.GIT_DIFF_CHECK);
        WorkflowEvidenceBundle bundle = service.evidence(created.id());

        assertThat(verification.success()).isTrue();
        assertThat(bundle.deterministicVerification()).isEqualTo(verification);
        assertThat(bundle.workflow().acceptanceCriteria())
                .containsExactly("Repository checks pass", "Evidence bundle exposes Git state");
        assertThat(bundle.workflow().stageOutputs()).isNotEmpty();
        assertThat(bundle.git().available()).isTrue();
        assertThat(bundle.git().status().output()).contains("new-file.txt");
    }

    private Path createGitWorkspace(String id) throws Exception {
        Path root = tempDir.resolve("workspaces");
        Files.createDirectories(root);
        Path workspace = Files.createDirectories(root.resolve(id));
        Process process = new ProcessBuilder("git", "init", "--quiet")
                .directory(workspace.toFile())
                .start();
        assertThat(process.waitFor()).isZero();
        return workspace;
    }

    private WorkspaceService workspaceService() {
        GatewayProperties properties = new GatewayProperties();
        properties.getCodex().setWorkspaceRoot(tempDir.resolve("workspaces").toString());
        return new WorkspaceService(properties);
    }

    private WorkflowSnapshot awaitStatus(String workflowId, WorkflowStatus expected) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(2));
        WorkflowSnapshot snapshot = workflows.get(workflowId);
        while (snapshot.status() != expected && Instant.now().isBefore(deadline)) {
            Thread.sleep(10);
            snapshot = workflows.get(workflowId);
        }
        assertThat(snapshot.status()).isEqualTo(expected);
        return snapshot;
    }
}

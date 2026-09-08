package io.github.son1004007.codexremote.workflow;

import io.github.son1004007.codexremote.config.GatewayProperties;
import io.github.son1004007.codexremote.workspace.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowVerificationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void runsBoundedMavenWrapperAndCapturesIndependentResult() throws Exception {
        Path workspace = createWorkspace("demo");
        Path wrapper = workspace.resolve("mvnw");
        Files.writeString(wrapper, "#!/bin/sh\necho deterministic-verifier-ok\nexit 0\n");
        assertThat(wrapper.toFile().setExecutable(true)).isTrue();

        WorkflowVerificationService service = new WorkflowVerificationService(workspaceService());
        WorkflowVerificationResult result = service.verify("demo", VerificationProfile.MAVEN_TEST);

        assertThat(result.success()).isTrue();
        assertThat(result.exitCode()).isZero();
        assertThat(result.command()).containsExactly("./mvnw", "-q", "test");
        assertThat(result.output()).contains("deterministic-verifier-ok");
    }

    @Test
    void recordsVerifierFailureWithoutConvertingItToSuccess() throws Exception {
        Path workspace = createWorkspace("failed");
        Path wrapper = workspace.resolve("mvnw");
        Files.writeString(wrapper, "#!/bin/sh\necho deterministic-verifier-failed\nexit 7\n");
        assertThat(wrapper.toFile().setExecutable(true)).isTrue();

        WorkflowVerificationService service = new WorkflowVerificationService(workspaceService());
        WorkflowVerificationResult result = service.verify("failed", VerificationProfile.MAVEN_TEST);

        assertThat(result.success()).isFalse();
        assertThat(result.exitCode()).isEqualTo(7);
        assertThat(result.output()).contains("deterministic-verifier-failed");
    }

    private Path createWorkspace(String id) throws Exception {
        Path root = tempDir.resolve("workspaces");
        Files.createDirectories(root);
        return Files.createDirectories(root.resolve(id));
    }

    private WorkspaceService workspaceService() {
        GatewayProperties properties = new GatewayProperties();
        properties.getCodex().setWorkspaceRoot(tempDir.resolve("workspaces").toString());
        return new WorkspaceService(properties);
    }
}

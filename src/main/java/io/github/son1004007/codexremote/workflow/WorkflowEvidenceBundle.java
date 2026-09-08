package io.github.son1004007.codexremote.workflow;

import java.time.Instant;

public record WorkflowEvidenceBundle(
        WorkflowSnapshot workflow,
        WorkflowVerificationResult deterministicVerification,
        WorkspaceGitEvidence git,
        Instant generatedAt
) {
}

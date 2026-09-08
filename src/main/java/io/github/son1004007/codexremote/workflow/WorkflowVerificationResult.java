package io.github.son1004007.codexremote.workflow;

import java.time.Instant;
import java.util.List;

public record WorkflowVerificationResult(
        VerificationProfile profile,
        List<String> command,
        int exitCode,
        boolean success,
        String output,
        boolean truncated,
        Instant startedAt,
        Instant completedAt
) {
}

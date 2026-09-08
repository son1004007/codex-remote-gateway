package io.github.son1004007.codexremote.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflows")
@ConditionalOnProperty(name = "gateway.agent.mode", havingValue = "codex")
public class WorkflowEvidenceController {

    private final WorkflowEvidenceService evidence;

    public WorkflowEvidenceController(WorkflowEvidenceService evidence) {
        this.evidence = evidence;
    }

    @PostMapping("/{workflowId}/verify")
    public WorkflowVerificationResult verify(
            @PathVariable String workflowId,
            @Valid @RequestBody VerifyWorkflowRequest request
    ) {
        return evidence.verify(workflowId, request.profile());
    }

    @GetMapping("/{workflowId}/evidence")
    public WorkflowEvidenceBundle evidence(@PathVariable String workflowId) {
        return evidence.evidence(workflowId);
    }

    public record VerifyWorkflowRequest(@NotNull VerificationProfile profile) {
    }
}

package io.github.son1004007.codexremote.workflow;

import io.github.son1004007.codexremote.workspace.WorkspaceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "gateway.agent.mode", havingValue = "codex")
public class WorkflowEvidenceService {

    private final WorkflowService workflows;
    private final WorkspaceService workspaces;
    private final WorkflowVerificationService verifier;
    private final Map<String, WorkflowVerificationResult> latestVerificationByWorkflow = new ConcurrentHashMap<>();

    public WorkflowEvidenceService(
            WorkflowService workflows,
            WorkspaceService workspaces,
            WorkflowVerificationService verifier
    ) {
        this.workflows = workflows;
        this.workspaces = workspaces;
        this.verifier = verifier;
    }

    public WorkflowVerificationResult verify(String workflowId, VerificationProfile profile) {
        WorkflowSnapshot snapshot = workflows.get(workflowId);
        ensureStableForVerification(snapshot);
        WorkflowVerificationResult result = verifier.verify(snapshot.workspaceId(), profile);
        latestVerificationByWorkflow.put(workflowId, result);
        return result;
    }

    public WorkflowEvidenceBundle evidence(String workflowId) {
        WorkflowSnapshot snapshot = workflows.get(workflowId);
        return new WorkflowEvidenceBundle(
                snapshot,
                latestVerificationByWorkflow.get(workflowId),
                gitEvidence(snapshot.workspaceId()),
                Instant.now()
        );
    }

    private WorkspaceGitEvidence gitEvidence(String workspaceId) {
        try {
            return new WorkspaceGitEvidence(
                    true,
                    workspaces.gitStatus(workspaceId),
                    workspaces.gitDiff(workspaceId, false),
                    workspaces.gitDiff(workspaceId, true),
                    null
            );
        } catch (RuntimeException ex) {
            return WorkspaceGitEvidence.unavailable(truncate(ex.getMessage()));
        }
    }

    private static void ensureStableForVerification(WorkflowSnapshot snapshot) {
        WorkflowStatus status = snapshot.status();
        if (status != WorkflowStatus.WAITING_APPROVAL
                && status != WorkflowStatus.BLOCKED
                && status != WorkflowStatus.FAILED
                && status != WorkflowStatus.COMPLETED) {
            throw new WorkflowStateException(
                    "Deterministic verification requires a stable workflow state; current status=" + status);
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return "Unknown Git evidence error";
        }
        String normalized = value.strip();
        return normalized.length() <= 1_000 ? normalized : normalized.substring(0, 1_000) + "...";
    }
}

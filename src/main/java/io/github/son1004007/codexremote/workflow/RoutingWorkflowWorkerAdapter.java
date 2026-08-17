package io.github.son1004007.codexremote.workflow;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "gateway.agent.mode", havingValue = "codex")
public class RoutingWorkflowWorkerAdapter implements WorkflowWorkerPort {

    private final CodexWorkflowWorkerAdapter codex;
    private final AntigravityWorkflowWorkerAdapter antigravity;

    public RoutingWorkflowWorkerAdapter(
            CodexWorkflowWorkerAdapter codex,
            ObjectProvider<AntigravityWorkflowWorkerAdapter> antigravityProvider
    ) {
        this.codex = codex;
        this.antigravity = antigravityProvider.getIfAvailable();
    }

    @Override
    public String provider() {
        return antigravity == null ? "codex" : "codex+antigravity";
    }

    @Override
    public String providerFor(WorkflowStage stage) {
        return workerFor(stage).provider();
    }

    @Override
    public Result execute(String workflowId, String workspaceId, WorkflowStage stage, String instruction) {
        return workerFor(stage).execute(workflowId, workspaceId, stage, instruction);
    }

    @Override
    public void cancel(String workflowId) {
        codex.cancel(workflowId);
        if (antigravity != null) {
            antigravity.cancel(workflowId);
        }
    }

    private WorkflowStageWorker workerFor(WorkflowStage stage) {
        if (antigravity != null && switch (stage) {
            case PLAN, TEST_DESIGN, REVIEW -> true;
            default -> false;
        }) {
            return antigravity;
        }
        return codex;
    }
}

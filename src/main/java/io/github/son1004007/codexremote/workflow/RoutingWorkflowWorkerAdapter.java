package io.github.son1004007.codexremote.workflow;

import io.github.son1004007.codexremote.config.GatewayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "gateway.agent.mode", havingValue = "codex")
public class RoutingWorkflowWorkerAdapter implements WorkflowWorkerPort {

    private final CodexWorkflowWorkerAdapter codex;
    private final AntigravityWorkflowWorkerAdapter antigravity;
    private final boolean antigravityEnabled;

    public RoutingWorkflowWorkerAdapter(
            CodexWorkflowWorkerAdapter codex,
            AntigravityWorkflowWorkerAdapter antigravity,
            GatewayProperties properties
    ) {
        this.codex = codex;
        this.antigravity = antigravity;
        this.antigravityEnabled = properties.getAntigravity().isEnabled();
    }

    @Override
    public String provider() {
        return antigravityEnabled ? "codex+antigravity" : "codex";
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
        if (antigravityEnabled) {
            antigravity.cancel(workflowId);
        }
    }

    private WorkflowStageWorker workerFor(WorkflowStage stage) {
        if (antigravityEnabled && switch (stage) {
            case PLAN, TEST_DESIGN, REVIEW -> true;
            default -> false;
        }) {
            return antigravity;
        }
        return codex;
    }
}

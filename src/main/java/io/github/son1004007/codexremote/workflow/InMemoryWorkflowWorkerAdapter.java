package io.github.son1004007.codexremote.workflow;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "gateway.agent.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryWorkflowWorkerAdapter implements WorkflowWorkerPort {

    @Override
    public String provider() {
        return "in-memory";
    }

    @Override
    public Result execute(String workflowId, String workspaceId, WorkflowStage stage, String instruction) {
        return new Result(
                "in-memory-" + workflowId,
                Outcome.SUCCESS,
                "Simulated " + stage + " stage for workspace " + workspaceId
        );
    }

    @Override
    public void cancel(String workflowId) {
        // No external worker exists in in-memory mode.
    }
}

package io.github.son1004007.codexremote.workflow;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingWorkflowWorkerAdapterTest {

    @Test
    void routesAnalysisAndAdversarialStagesToAntigravity() {
        RecordingWorker codex = new RecordingWorker("codex");
        RecordingWorker antigravity = new RecordingWorker("antigravity");
        RoutingWorkflowWorkerAdapter router = new RoutingWorkflowWorkerAdapter(codex, antigravity, true);

        for (WorkflowStage stage : WorkflowStage.values()) {
            router.execute("wf", "demo", stage, "instruction");
        }

        assertThat(antigravity.stages)
                .containsExactly(WorkflowStage.PLAN, WorkflowStage.TEST_DESIGN, WorkflowStage.REVIEW);
        assertThat(codex.stages)
                .containsExactly(
                        WorkflowStage.PLAN_VERIFY,
                        WorkflowStage.IMPLEMENT,
                        WorkflowStage.TEST,
                        WorkflowStage.REVIEW_VERIFY,
                        WorkflowStage.DEPLOY,
                        WorkflowStage.E2E
                );
    }

    @Test
    void fallsBackToCodexForEveryStageWhenAntigravityIsDisabled() {
        RecordingWorker codex = new RecordingWorker("codex");
        RecordingWorker antigravity = new RecordingWorker("antigravity");
        RoutingWorkflowWorkerAdapter router = new RoutingWorkflowWorkerAdapter(codex, antigravity, false);

        for (WorkflowStage stage : WorkflowStage.values()) {
            assertThat(router.providerFor(stage)).isEqualTo("codex");
            router.execute("wf", "demo", stage, "instruction");
        }

        assertThat(codex.stages).containsExactly(WorkflowStage.values());
        assertThat(antigravity.stages).isEmpty();
    }

    private static final class RecordingWorker implements WorkflowStageWorker {
        private final String provider;
        private final List<WorkflowStage> stages = new ArrayList<>();

        private RecordingWorker(String provider) {
            this.provider = provider;
        }

        @Override
        public String provider() {
            return provider;
        }

        @Override
        public WorkflowWorkerPort.Result execute(
                String workflowId,
                String workspaceId,
                WorkflowStage stage,
                String instruction
        ) {
            stages.add(stage);
            return new WorkflowWorkerPort.Result(
                    provider + "-session",
                    WorkflowWorkerPort.Outcome.SUCCESS,
                    provider + " result\nWORKFLOW_RESULT: SUCCESS"
            );
        }

        @Override
        public void cancel(String workflowId) {
        }
    }
}

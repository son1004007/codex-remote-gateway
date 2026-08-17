package io.github.son1004007.codexremote.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowHandoffTest {

    private final CapturingWorker worker = new CapturingWorker();
    private final WorkflowService service = new WorkflowService(worker);

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void passesPlanningAndReviewEvidenceAcrossIndependentWorkers() throws Exception {
        WorkflowSnapshot created = service.create("demo", "Implement safely", true);
        WorkflowSnapshot completed = awaitCompleted(created.id());

        assertThat(worker.instructions.get(WorkflowStage.PLAN_VERIFY))
                .contains("--- PLAN by gemini ---")
                .contains("PLAN evidence");
        assertThat(worker.instructions.get(WorkflowStage.IMPLEMENT))
                .contains("--- PLAN_VERIFY by codex ---")
                .contains("PLAN_VERIFY evidence");
        assertThat(worker.instructions.get(WorkflowStage.TEST))
                .contains("--- TEST_DESIGN by gemini ---")
                .contains("TEST_DESIGN evidence");
        assertThat(worker.instructions.get(WorkflowStage.REVIEW_VERIFY))
                .contains("--- REVIEW by gemini ---")
                .contains("REVIEW evidence");

        assertThat(completed.stageWorkers())
                .containsEntry(WorkflowStage.PLAN, "gemini")
                .containsEntry(WorkflowStage.PLAN_VERIFY, "codex")
                .containsEntry(WorkflowStage.IMPLEMENT, "codex")
                .containsEntry(WorkflowStage.TEST_DESIGN, "gemini")
                .containsEntry(WorkflowStage.REVIEW, "gemini")
                .containsEntry(WorkflowStage.REVIEW_VERIFY, "codex");
    }

    private WorkflowSnapshot awaitCompleted(String workflowId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(2));
        WorkflowSnapshot snapshot = service.get(workflowId);
        while (snapshot.status() != WorkflowStatus.COMPLETED && Instant.now().isBefore(deadline)) {
            Thread.sleep(10);
            snapshot = service.get(workflowId);
        }
        assertThat(snapshot.status()).isEqualTo(WorkflowStatus.COMPLETED);
        return snapshot;
    }

    private static final class CapturingWorker implements WorkflowWorkerPort {
        private final Map<WorkflowStage, String> instructions = new ConcurrentHashMap<>();

        @Override
        public String provider() {
            return "codex+gemini";
        }

        @Override
        public String providerFor(WorkflowStage stage) {
            return switch (stage) {
                case PLAN, TEST_DESIGN, REVIEW -> "gemini";
                default -> "codex";
            };
        }

        @Override
        public Result execute(String workflowId, String workspaceId, WorkflowStage stage, String instruction) {
            instructions.put(stage, instruction);
            return new Result(
                    providerFor(stage) + "-session",
                    Outcome.SUCCESS,
                    stage + " evidence\nWORKFLOW_RESULT: SUCCESS"
            );
        }

        @Override
        public void cancel(String workflowId) {
        }
    }
}

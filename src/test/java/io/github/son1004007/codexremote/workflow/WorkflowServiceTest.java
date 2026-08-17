package io.github.son1004007.codexremote.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowServiceTest {

    private final WorkflowService service = new WorkflowService(new InMemoryWorkflowWorkerAdapter());

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void runsToDeploymentGateThenCompletesAfterApproval() throws Exception {
        WorkflowSnapshot created = service.create("demo", "Implement a safe change", false);

        WorkflowSnapshot waiting = awaitStatus(created.id(), WorkflowStatus.WAITING_APPROVAL);
        assertThat(waiting.stage()).isEqualTo(WorkflowStage.DEPLOY);

        service.approve(created.id());

        WorkflowSnapshot completed = awaitStatus(created.id(), WorkflowStatus.COMPLETED);
        assertThat(completed.stage()).isEqualTo(WorkflowStage.E2E);
        assertThat(completed.lastError()).isNull();
    }

    @Test
    void explicitAutoDeploySkipsHumanGate() throws Exception {
        WorkflowSnapshot created = service.create("auto-demo", "Implement and deploy", true);

        WorkflowSnapshot completed = awaitStatus(created.id(), WorkflowStatus.COMPLETED);

        assertThat(completed.events())
                .noneMatch(event -> "APPROVAL_REQUIRED".equals(event.type()));
    }

    @Test
    void preventsTwoActiveWorkflowsForSameWorkspace() throws Exception {
        WorkflowSnapshot first = service.create("exclusive", "First task", false);
        awaitStatus(first.id(), WorkflowStatus.WAITING_APPROVAL);

        assertThatThrownBy(() -> service.create("exclusive", "Second task", false))
                .isInstanceOf(WorkflowWorkspaceConflictException.class);

        service.cancel(first.id());
    }

    private WorkflowSnapshot awaitStatus(String workflowId, WorkflowStatus expected) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(2));
        WorkflowSnapshot snapshot = service.get(workflowId);
        while (snapshot.status() != expected && Instant.now().isBefore(deadline)) {
            Thread.sleep(10);
            snapshot = service.get(workflowId);
        }
        assertThat(snapshot.status()).isEqualTo(expected);
        return snapshot;
    }
}

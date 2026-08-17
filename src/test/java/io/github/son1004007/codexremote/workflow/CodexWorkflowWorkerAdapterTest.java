package io.github.son1004007.codexremote.workflow;

import io.github.son1004007.codexremote.session.AgentSession;
import io.github.son1004007.codexremote.session.AgentSessionPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodexWorkflowWorkerAdapterTest {

    @Test
    void acceptsExplicitSuccessMarker() {
        StubSessionPort sessions = new StubSessionPort("Implemented and tested\nWORKFLOW_RESULT: SUCCESS");
        CodexWorkflowWorkerAdapter adapter = new CodexWorkflowWorkerAdapter(sessions);

        WorkflowWorkerPort.Result result = adapter.execute("wf-1", "demo", WorkflowStage.TEST, "test");

        assertThat(result.outcome()).isEqualTo(WorkflowWorkerPort.Outcome.SUCCESS);
    }

    @Test
    void missingMarkerFailsClosedAsBlocked() {
        StubSessionPort sessions = new StubSessionPort("Looks good to me");
        CodexWorkflowWorkerAdapter adapter = new CodexWorkflowWorkerAdapter(sessions);

        WorkflowWorkerPort.Result result = adapter.execute("wf-2", "demo", WorkflowStage.REVIEW, "review");

        assertThat(result.outcome()).isEqualTo(WorkflowWorkerPort.Outcome.BLOCKED);
    }

    @Test
    void failedMarkerIsNotPromotedToSuccess() {
        StubSessionPort sessions = new StubSessionPort("Build failed\nWORKFLOW_RESULT: FAILED - compilation error");
        CodexWorkflowWorkerAdapter adapter = new CodexWorkflowWorkerAdapter(sessions);

        WorkflowWorkerPort.Result result = adapter.execute("wf-3", "demo", WorkflowStage.TEST, "test");

        assertThat(result.outcome()).isEqualTo(WorkflowWorkerPort.Outcome.FAILED);
    }

    private static final class StubSessionPort implements AgentSessionPort {
        private final String response;
        private final List<AgentSession> sessions = new ArrayList<>();

        private StubSessionPort(String response) {
            this.response = response;
        }

        @Override
        public AgentSession create(String workspaceId) {
            AgentSession session = new AgentSession("session-" + (sessions.size() + 1), workspaceId);
            sessions.add(session);
            return session;
        }

        @Override
        public AgentSession get(String sessionId) {
            return sessions.stream()
                    .filter(session -> session.id().equals(sessionId))
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public AgentSession submit(String sessionId, String input) {
            AgentSession session = get(sessionId);
            session.submit(input);
            session.addAssistantMessage(response);
            return session;
        }

        @Override
        public AgentSession cancel(String sessionId) {
            AgentSession session = get(sessionId);
            session.cancel();
            return session;
        }

        @Override
        public List<AgentSession> list() {
            return List.copyOf(sessions);
        }
    }
}

package io.github.son1004007.codexremote.workflow;

import io.github.son1004007.codexremote.session.AgentSession;
import io.github.son1004007.codexremote.session.AgentSessionPort;
import io.github.son1004007.codexremote.session.SessionEvent;
import io.github.son1004007.codexremote.session.SessionStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "gateway.agent.mode", havingValue = "codex")
public class CodexWorkflowWorkerAdapter implements WorkflowWorkerPort {

    private static final String RESULT_PREFIX = "WORKFLOW_RESULT:";

    private final AgentSessionPort sessions;
    private final Map<String, String> sessionIdsByWorkflow = new ConcurrentHashMap<>();

    public CodexWorkflowWorkerAdapter(AgentSessionPort sessions) {
        this.sessions = sessions;
    }

    @Override
    public String provider() {
        return "codex";
    }

    @Override
    public Result execute(String workflowId, String workspaceId, WorkflowStage stage, String instruction) {
        String sessionId = sessionIdsByWorkflow.computeIfAbsent(
                workflowId,
                ignored -> sessions.create(workspaceId).id()
        );
        AgentSession updated = sessions.submit(sessionId, instruction);
        String message = lastAssistantMessage(updated);
        return new Result(sessionId, parseOutcome(message), message);
    }

    @Override
    public void cancel(String workflowId) {
        String sessionId = sessionIdsByWorkflow.remove(workflowId);
        if (sessionId == null) {
            return;
        }
        AgentSession session = sessions.get(sessionId);
        if (session.status() == SessionStatus.ACTIVE) {
            sessions.cancel(sessionId);
        }
    }

    private static String lastAssistantMessage(AgentSession session) {
        return session.events().stream()
                .filter(event -> "ASSISTANT".equals(event.actor()))
                .map(SessionEvent::message)
                .reduce((first, second) -> second)
                .orElse("");
    }

    private static Outcome parseOutcome(String message) {
        String marker = message.lines()
                .map(String::strip)
                .filter(line -> line.startsWith(RESULT_PREFIX))
                .reduce((first, second) -> second)
                .orElse("");

        String value = marker.substring(Math.min(marker.length(), RESULT_PREFIX.length())).strip();
        if (value.startsWith("SUCCESS")) {
            return Outcome.SUCCESS;
        }
        if (value.startsWith("FAILED")) {
            return Outcome.FAILED;
        }
        return Outcome.BLOCKED;
    }
}

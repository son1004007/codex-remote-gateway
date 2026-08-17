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
class CodexWorkflowWorkerAdapter implements WorkflowStageWorker {

    private final AgentSessionPort sessions;
    private final Map<String, String> sessionIdsByWorkflow = new ConcurrentHashMap<>();

    CodexWorkflowWorkerAdapter(AgentSessionPort sessions) {
        this.sessions = sessions;
    }

    @Override
    public String provider() {
        return "codex";
    }

    @Override
    public WorkflowWorkerPort.Result execute(
            String workflowId,
            String workspaceId,
            WorkflowStage stage,
            String instruction
    ) {
        String sessionId = sessionIdsByWorkflow.computeIfAbsent(
                workflowId,
                ignored -> sessions.create(workspaceId).id()
        );
        AgentSession updated = sessions.submit(sessionId, instruction);
        String message = lastAssistantMessage(updated);
        return new WorkflowWorkerPort.Result(sessionId, WorkflowResultParser.parse(message), message);
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
}

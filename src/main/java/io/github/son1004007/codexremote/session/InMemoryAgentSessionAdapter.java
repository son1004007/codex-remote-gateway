package io.github.son1004007.codexremote.session;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "gateway.agent.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryAgentSessionAdapter implements AgentSessionPort {

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> activeSessionIdsByWorkspace = new ConcurrentHashMap<>();

    @Override
    public AgentSession create(String workspaceId) {
        String id = UUID.randomUUID().toString();
        if (activeSessionIdsByWorkspace.putIfAbsent(workspaceId, id) != null) {
            throw new WorkspaceSessionConflictException(workspaceId);
        }
        AgentSession session = new AgentSession(id, workspaceId);
        sessions.put(id, session);
        return session;
    }

    @Override
    public AgentSession get(String sessionId) {
        AgentSession session = sessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException(sessionId);
        }
        return session;
    }

    @Override
    public AgentSession submit(String sessionId, String input) {
        AgentSession session = get(sessionId);
        session.submit(input);
        return session;
    }

    @Override
    public AgentSession cancel(String sessionId) {
        AgentSession session = get(sessionId);
        session.cancel();
        activeSessionIdsByWorkspace.remove(session.workspaceId(), session.id());
        return session;
    }

    @Override
    public List<AgentSession> list() {
        return sessions.values().stream()
                .sorted(Comparator.comparing(AgentSession::createdAt).reversed())
                .toList();
    }
}

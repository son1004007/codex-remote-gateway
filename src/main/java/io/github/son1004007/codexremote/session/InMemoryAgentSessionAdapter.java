package io.github.son1004007.codexremote.session;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryAgentSessionAdapter implements AgentSessionPort {

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();

    @Override
    public AgentSession create(String workspaceId) {
        String id = UUID.randomUUID().toString();
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
        return session;
    }

    @Override
    public List<AgentSession> list() {
        return sessions.values().stream()
                .sorted(Comparator.comparing(AgentSession::createdAt).reversed())
                .toList();
    }
}

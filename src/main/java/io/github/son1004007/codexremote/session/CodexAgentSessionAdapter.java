package io.github.son1004007.codexremote.session;

import io.github.son1004007.codexremote.config.GatewayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "gateway.agent.mode", havingValue = "codex")
public class CodexAgentSessionAdapter implements AgentSessionPort {

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();
    private final CodexAppServerClient client;
    private final Path workspaceRoot;

    public CodexAgentSessionAdapter(CodexAppServerClient client, GatewayProperties properties) {
        this.client = client;
        this.workspaceRoot = Path.of(properties.getCodex().getWorkspaceRoot()).toAbsolutePath().normalize();
    }

    @Override
    public AgentSession create(String workspaceId) {
        resolveWorkspace(workspaceId);
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
        synchronized (session) {
            session.submit(input);
            Path workspace = resolveWorkspace(session.workspaceId());
            try {
                CodexAppServerClient.TurnResult result = client.execute(session.providerThreadId(), workspace, input);
                session.bindProviderThreadId(result.threadId());
                session.addAssistantMessage(result.message());
                return session;
            } catch (CodexExecutionException ex) {
                session.addSystemEvent("CODEX_ERROR", ex.getMessage());
                throw ex;
            }
        }
    }

    @Override
    public AgentSession cancel(String sessionId) {
        AgentSession session = get(sessionId);
        synchronized (session) {
            session.cancel();
            return session;
        }
    }

    @Override
    public List<AgentSession> list() {
        return sessions.values().stream()
                .sorted(Comparator.comparing(AgentSession::createdAt).reversed())
                .toList();
    }

    private Path resolveWorkspace(String workspaceId) {
        Path candidate = workspaceRoot.resolve(workspaceId).normalize();
        if (!candidate.startsWith(workspaceRoot) || !Files.isDirectory(candidate)) {
            throw new WorkspaceNotFoundException(workspaceId);
        }
        return candidate;
    }
}

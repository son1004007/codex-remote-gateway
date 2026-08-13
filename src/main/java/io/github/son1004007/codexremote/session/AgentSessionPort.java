package io.github.son1004007.codexremote.session;

import java.util.List;

public interface AgentSessionPort {

    AgentSession create(String workspaceId);

    AgentSession get(String sessionId);

    AgentSession submit(String sessionId, String input);

    AgentSession cancel(String sessionId);

    List<AgentSession> list();
}

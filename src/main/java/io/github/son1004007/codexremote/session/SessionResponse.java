package io.github.son1004007.codexremote.session;

import java.time.Instant;
import java.util.List;

public record SessionResponse(
        String id,
        String workspaceId,
        String providerThreadId,
        SessionStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<SessionEvent> events
) {
    public static SessionResponse from(AgentSession session) {
        return new SessionResponse(
                session.id(),
                session.workspaceId(),
                session.providerThreadId(),
                session.status(),
                session.createdAt(),
                session.updatedAt(),
                session.events()
        );
    }
}

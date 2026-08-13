package io.github.son1004007.codexremote.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AgentSession {

    private final String id;
    private final String workspaceId;
    private String providerThreadId;
    private SessionStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<SessionEvent> events = new ArrayList<>();

    public AgentSession(String id, String workspaceId) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.status = SessionStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.events.add(SessionEvent.system("SESSION_STARTED", "Session created"));
    }

    public String id() {
        return id;
    }

    public String workspaceId() {
        return workspaceId;
    }

    public String providerThreadId() {
        return providerThreadId;
    }

    public SessionStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<SessionEvent> events() {
        return Collections.unmodifiableList(events);
    }

    public void submit(String input) {
        if (status != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Only active sessions accept input");
        }
        events.add(SessionEvent.user("USER_INPUT", input));
        updatedAt = Instant.now();
    }

    public void bindProviderThreadId(String providerThreadId) {
        if (providerThreadId == null || providerThreadId.isBlank()) {
            throw new IllegalArgumentException("providerThreadId must not be blank");
        }
        if (this.providerThreadId != null && !this.providerThreadId.equals(providerThreadId)) {
            throw new IllegalStateException("Provider thread cannot change after it is bound");
        }
        this.providerThreadId = providerThreadId;
        updatedAt = Instant.now();
    }

    public void addAssistantMessage(String message) {
        events.add(SessionEvent.assistant("AGENT_MESSAGE", message == null ? "" : message));
        updatedAt = Instant.now();
    }

    public void addSystemEvent(String type, String message) {
        events.add(SessionEvent.system(type, message));
        updatedAt = Instant.now();
    }

    public void cancel() {
        if (status == SessionStatus.CANCELLED) {
            return;
        }
        if (status != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Only active sessions can be cancelled");
        }
        status = SessionStatus.CANCELLED;
        events.add(SessionEvent.system("SESSION_CANCELLED", "Session cancelled"));
        updatedAt = Instant.now();
    }
}

package io.github.son1004007.codexremote.session;

import java.time.Instant;

public record SessionEvent(
        String type,
        String actor,
        String message,
        Instant occurredAt
) {
    public static SessionEvent system(String type, String message) {
        return new SessionEvent(type, "SYSTEM", message, Instant.now());
    }

    public static SessionEvent user(String type, String message) {
        return new SessionEvent(type, "USER", message, Instant.now());
    }
}

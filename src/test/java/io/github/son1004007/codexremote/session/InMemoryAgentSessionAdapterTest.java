package io.github.son1004007.codexremote.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAgentSessionAdapterTest {

    private final InMemoryAgentSessionAdapter adapter = new InMemoryAgentSessionAdapter();

    @Test
    void submitAddsUserEvent() {
        AgentSession session = adapter.create("demo");

        AgentSession updated = adapter.submit(session.id(), "implement feature");

        assertThat(updated.events()).hasSize(2);
        assertThat(updated.events().get(1).type()).isEqualTo("USER_INPUT");
        assertThat(updated.events().get(1).message()).isEqualTo("implement feature");
    }

    @Test
    void cancelledSessionRejectsFurtherInput() {
        AgentSession session = adapter.create("demo");
        adapter.cancel(session.id());

        assertThat(adapter.get(session.id()).status()).isEqualTo(SessionStatus.CANCELLED);
        assertThatThrownBy(() -> adapter.submit(session.id(), "should fail"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active sessions");
    }

    @Test
    void unknownSessionFails() {
        assertThatThrownBy(() -> adapter.get("missing"))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void allowsOnlyOneActiveSessionPerWorkspace() {
        AgentSession first = adapter.create("demo");

        assertThatThrownBy(() -> adapter.create("demo"))
                .isInstanceOf(WorkspaceSessionConflictException.class);

        adapter.cancel(first.id());

        assertThat(adapter.create("demo").workspaceId()).isEqualTo("demo");
    }
}

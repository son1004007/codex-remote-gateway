package io.github.son1004007.codexremote.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final AgentSessionPort sessions;

    public SessionController(AgentSessionPort sessions) {
        this.sessions = sessions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentSession create(@Valid @RequestBody CreateSessionRequest request) {
        return sessions.create(request.workspaceId());
    }

    @GetMapping
    public List<AgentSession> list() {
        return sessions.list();
    }

    @GetMapping("/{sessionId}")
    public AgentSession get(@PathVariable String sessionId) {
        return sessions.get(sessionId);
    }

    @PostMapping("/{sessionId}/messages")
    public AgentSession submit(
            @PathVariable String sessionId,
            @Valid @RequestBody SubmitMessageRequest request
    ) {
        return sessions.submit(sessionId, request.input());
    }

    @PostMapping("/{sessionId}/cancel")
    public AgentSession cancel(@PathVariable String sessionId) {
        return sessions.cancel(sessionId);
    }

    public record CreateSessionRequest(@NotBlank String workspaceId) {
    }

    public record SubmitMessageRequest(@NotBlank String input) {
    }
}

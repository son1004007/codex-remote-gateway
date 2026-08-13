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
    public SessionResponse create(@Valid @RequestBody CreateSessionRequest request) {
        return SessionResponse.from(sessions.create(request.workspaceId()));
    }

    @GetMapping
    public List<SessionResponse> list() {
        return sessions.list().stream()
                .map(SessionResponse::from)
                .toList();
    }

    @GetMapping("/{sessionId}")
    public SessionResponse get(@PathVariable String sessionId) {
        return SessionResponse.from(sessions.get(sessionId));
    }

    @PostMapping("/{sessionId}/messages")
    public SessionResponse submit(
            @PathVariable String sessionId,
            @Valid @RequestBody SubmitMessageRequest request
    ) {
        return SessionResponse.from(sessions.submit(sessionId, request.input()));
    }

    @PostMapping("/{sessionId}/cancel")
    public SessionResponse cancel(@PathVariable String sessionId) {
        return SessionResponse.from(sessions.cancel(sessionId));
    }

    public record CreateSessionRequest(@NotBlank String workspaceId) {
    }

    public record SubmitMessageRequest(@NotBlank String input) {
    }
}

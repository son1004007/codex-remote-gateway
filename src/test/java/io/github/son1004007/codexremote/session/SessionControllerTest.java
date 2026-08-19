package io.github.son1004007.codexremote.session;

import io.github.son1004007.codexremote.config.GatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    GatewayProperties gatewayProperties;

    @Test
    void usesCurrentCodexSandboxTokenByDefault() {
        assertThat(gatewayProperties.getCodex().getSandbox()).isEqualTo("workspace-write");
    }

    @Test
    void servesBrowserControlUi() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Codex Remote Gateway")));
    }

    @Test
    void createsSession() throws Exception {
        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workspaceId\":\"demo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.workspaceId").value("demo"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.events[0].type").value("SESSION_STARTED"));
    }

    @Test
    void acceptsTurnAsynchronouslyAndExposesExecutionState() throws Exception {
        String createBody = mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workspaceId\":\"async-demo\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Matcher matcher = ID_PATTERN.matcher(createBody);
        assertThat(matcher.find()).isTrue();
        String sessionId = matcher.group(1);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"hello\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sessionId").value(sessionId));

        for (int i = 0; i < 20; i++) {
            String body = mockMvc.perform(get("/api/v1/sessions/{sessionId}/execution", sessionId))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            if (body.contains("\"status\":\"SUCCEEDED\"")) {
                break;
            }
            Thread.sleep(10);
        }

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/execution", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.running").value(false));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[1].actor").value("USER"))
                .andExpect(jsonPath("$.events[1].message").value("hello"));
    }

    @Test
    void rejectsSecondActiveSessionForSameWorkspace() throws Exception {
        String request = "{\"workspaceId\":\"exclusive-workspace\"}";

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid session state"));
    }

    @Test
    void rejectsBlankWorkspace() throws Exception {
        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workspaceId\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownSession() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{sessionId}", "missing"))
                .andExpect(status().isNotFound());
    }
}

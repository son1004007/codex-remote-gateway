package io.github.son1004007.codexremote.session;

import io.github.son1004007.codexremote.config.GatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    GatewayProperties gatewayProperties;

    @Test
    void usesCurrentCodexSandboxTokenByDefault() {
        assertThat(gatewayProperties.getCodex().getSandbox()).isEqualTo("workspace-write");
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

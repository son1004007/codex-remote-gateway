package io.github.son1004007.codexremote.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createSubmitCancelLifecycle() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workspaceId\":\"demo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workspaceId").value("demo"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);
        String sessionId = created.get("id").asText();
        assertThat(sessionId).isNotBlank();

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"implement feature\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[1].type").value("USER_INPUT"))
                .andExpect(jsonPath("$.events[1].message").value("implement feature"));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/cancel", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"should fail\"}"))
                .andExpect(status().isConflict());
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

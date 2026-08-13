package io.github.son1004007.codexremote.session;

import io.github.son1004007.codexremote.config.GatewayProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledOnOs(OS.LINUX)
class CodexAppServerClientTest {

    @TempDir
    Path tempDir;

    @Test
    void startsThreadAndCompletesTurn() throws Exception {
        Path fakeCodex = writeFakeCodex();
        GatewayProperties properties = properties(fakeCodex);
        CodexAppServerClient client = new CodexAppServerClient(properties, new ObjectMapper());

        CodexAppServerClient.TurnResult result = client.execute(null, tempDir, "hello");

        assertThat(result.threadId()).isEqualTo("thread-test");
        assertThat(result.message()).isEqualTo("gateway-ok");
    }

    @Test
    void resumesExistingThread() throws Exception {
        Path fakeCodex = writeFakeCodex();
        GatewayProperties properties = properties(fakeCodex);
        CodexAppServerClient client = new CodexAppServerClient(properties, new ObjectMapper());

        CodexAppServerClient.TurnResult result = client.execute("thread-test", tempDir, "again");

        assertThat(result.threadId()).isEqualTo("thread-test");
        assertThat(result.message()).isEqualTo("gateway-ok");
    }

    @Test
    void mapsInvalidThreadResumeToCodexExecutionFailure() throws Exception {
        Path fakeCodex = writeFakeCodex();
        CodexAppServerClient client = new CodexAppServerClient(properties(fakeCodex), new ObjectMapper());

        assertThatThrownBy(() -> client.execute("missing-thread", tempDir, "again"))
                .isInstanceOf(CodexExecutionException.class)
                .hasMessageContaining("request failed");
    }

    @Test
    void mapsCodexProcessStartFailureToCodexExecutionFailure() {
        CodexAppServerClient client = new CodexAppServerClient(properties(tempDir.resolve("missing-codex")), new ObjectMapper());

        assertThatThrownBy(() -> client.execute(null, tempDir, "hello"))
                .isInstanceOf(CodexExecutionException.class)
                .hasMessageContaining("Failed to start Codex app-server");
    }

    private GatewayProperties properties(Path fakeCodex) {
        GatewayProperties properties = new GatewayProperties();
        properties.getCodex().setCommand(fakeCodex.toAbsolutePath().toString());
        properties.getCodex().setCodexHome(tempDir.resolve("codex-home").toString());
        properties.getCodex().setTurnTimeout(Duration.ofSeconds(5));
        return properties;
    }

    private Path writeFakeCodex() throws Exception {
        Path script = tempDir.resolve("fake-codex.sh");
        Files.writeString(script, """
                #!/usr/bin/env bash
                set -euo pipefail
                [ "${1:-}" = "app-server" ] || exit 64
                while IFS= read -r line; do
                  case "$line" in
                    *'"method":"initialize"'*)
                      printf '%s\\n' '{"id":1,"result":{"userAgent":"fake"}}'
                      ;;
                    *'"method":"initialized"'*)
                      ;;
                    *'"method":"thread/start"'*)
                      case "$line" in
                        *'"sandbox":"workspace-write"'*)
                          printf '%s\\n' '{"id":2,"result":{"thread":{"id":"thread-test"}}}'
                          ;;
                        *)
                          printf '%s\\n' '{"id":2,"error":{"message":"sandbox must use the current Codex CLI token"}}'
                          ;;
                      esac
                      ;;
                    *'"method":"thread/resume"'*)
                      case "$line" in
                        *'"threadId":"missing-thread"'*)
                          printf '%s\\n' '{"id":2,"error":{"message":"no rollout found"}}'
                          ;;
                        *'"excludeTurns"'*)
                          printf '%s\\n' '{"id":2,"error":{"message":"excludeTurns requires experimentalApi"}}'
                          ;;
                        *)
                          printf '%s\\n' '{"id":2,"result":{"thread":{"id":"thread-test"}}}'
                          ;;
                      esac
                      ;;
                    *'"method":"turn/start"'*)
                      printf '%s\\n' '{"id":3,"result":{"turn":{"id":"turn-test","status":"inProgress","items":[],"error":null}}}'
                      printf '%s\\n' '{"method":"item/agentMessage/delta","params":{"threadId":"thread-test","turnId":"turn-test","itemId":"item-test","delta":"gateway-ok"}}'
                      printf '%s\\n' '{"method":"turn/completed","params":{"threadId":"thread-test","turn":{"id":"turn-test","status":"completed","items":[],"error":null}}}'
                      ;;
                  esac
                done
                """);
        assertThat(script.toFile().setExecutable(true)).isTrue();
        return script;
    }
}

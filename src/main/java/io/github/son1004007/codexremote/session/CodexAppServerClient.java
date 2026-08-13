package io.github.son1004007.codexremote.session;

import io.github.son1004007.codexremote.config.GatewayProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class CodexAppServerClient {

    private static final String EOF = "__CODEX_APP_SERVER_EOF__";

    private final GatewayProperties properties;
    private final ObjectMapper objectMapper;

    public CodexAppServerClient(GatewayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public TurnResult execute(String existingThreadId, Path workspace, String input) {
        GatewayProperties.Codex codex = properties.getCodex();
        ProcessBuilder builder = new ProcessBuilder(List.of(codex.getCommand(), "app-server"));
        builder.directory(workspace.toFile());
        if (codex.getCodexHome() != null && !codex.getCodexHome().isBlank()) {
            builder.environment().put("CODEX_HOME", codex.getCodexHome());
        }

        Process process;
        try {
            process = builder.start();
        } catch (IOException ex) {
            throw new CodexExecutionException("Failed to start Codex app-server using command: " + codex.getCommand(), ex);
        }

        BlockingQueue<String> stdout = new LinkedBlockingQueue<>();
        StringBuilder stderr = new StringBuilder();
        Thread stdoutReader = startStdoutReader(process, stdout);
        Thread stderrReader = startStderrReader(process, stderr);
        StringBuilder streamedAnswer = new StringBuilder();
        StringBuilder completedAnswer = new StringBuilder();
        Instant deadline = Instant.now().plus(codex.getTurnTimeout());

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            sendInitialize(writer);
            readUntilResponse(1, stdout, process, writer, deadline, streamedAnswer, completedAnswer, stderr);
            sendInitialized(writer);

            String threadId;
            if (existingThreadId == null || existingThreadId.isBlank()) {
                sendThreadStart(writer, workspace, codex);
                JsonNode response = readUntilResponse(2, stdout, process, writer, deadline, streamedAnswer, completedAnswer, stderr);
                threadId = requiredText(response, "result", "thread", "id");
            } else {
                sendThreadResume(writer, existingThreadId, workspace, codex);
                JsonNode response = readUntilResponse(2, stdout, process, writer, deadline, streamedAnswer, completedAnswer, stderr);
                threadId = requiredText(response, "result", "thread", "id");
            }

            sendTurnStart(writer, threadId, input, codex);
            readUntilResponse(3, stdout, process, writer, deadline, streamedAnswer, completedAnswer, stderr);
            JsonNode completed = readUntilTurnCompleted(stdout, process, writer, deadline, streamedAnswer, completedAnswer, stderr);
            validateTurnCompleted(completed, stderr);

            String answer = streamedAnswer.isEmpty() ? completedAnswer.toString() : streamedAnswer.toString();
            return new TurnResult(threadId, answer.trim());
        } catch (IOException ex) {
            throw new CodexExecutionException("I/O failure while communicating with Codex app-server", ex);
        } finally {
            stopProcess(process);
            stdoutReader.interrupt();
            stderrReader.interrupt();
        }
    }

    private void sendInitialize(BufferedWriter writer) throws IOException {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("method", "initialize");
        request.put("id", 1);
        ObjectNode params = request.putObject("params");
        ObjectNode clientInfo = params.putObject("clientInfo");
        clientInfo.put("name", "codex_remote_gateway");
        clientInfo.put("title", "Codex Remote Gateway");
        clientInfo.put("version", "0.1.0");
        send(writer, request);
    }

    private void sendInitialized(BufferedWriter writer) throws IOException {
        ObjectNode notification = objectMapper.createObjectNode();
        notification.put("method", "initialized");
        send(writer, notification);
    }

    private void sendThreadStart(BufferedWriter writer, Path workspace, GatewayProperties.Codex codex) throws IOException {
        ObjectNode request = baseRequest(2, "thread/start");
        ObjectNode params = request.putObject("params");
        applyThreadOptions(params, workspace, codex);
        send(writer, request);
    }

    private void sendThreadResume(
            BufferedWriter writer,
            String threadId,
            Path workspace,
            GatewayProperties.Codex codex
    ) throws IOException {
        ObjectNode request = baseRequest(2, "thread/resume");
        ObjectNode params = request.putObject("params");
        params.put("threadId", threadId);
        params.put("excludeTurns", true);
        applyThreadOptions(params, workspace, codex);
        send(writer, request);
    }

    private void applyThreadOptions(ObjectNode params, Path workspace, GatewayProperties.Codex codex) {
        params.put("cwd", workspace.toAbsolutePath().normalize().toString());
        params.put("approvalPolicy", codex.getApprovalPolicy());
        params.put("sandbox", codex.getSandbox());
        if (codex.getModel() != null && !codex.getModel().isBlank()) {
            params.put("model", codex.getModel());
        }
    }

    private void sendTurnStart(
            BufferedWriter writer,
            String threadId,
            String input,
            GatewayProperties.Codex codex
    ) throws IOException {
        ObjectNode request = baseRequest(3, "turn/start");
        ObjectNode params = request.putObject("params");
        params.put("threadId", threadId);
        params.put("approvalPolicy", codex.getApprovalPolicy());
        if (codex.getModel() != null && !codex.getModel().isBlank()) {
            params.put("model", codex.getModel());
        }
        ArrayNode inputs = params.putArray("input");
        ObjectNode text = inputs.addObject();
        text.put("type", "text");
        text.put("text", input);
        send(writer, request);
    }

    private ObjectNode baseRequest(int id, String method) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("method", method);
        request.put("id", id);
        return request;
    }

    private void send(BufferedWriter writer, JsonNode node) throws IOException {
        writer.write(objectMapper.writeValueAsString(node));
        writer.newLine();
        writer.flush();
    }

    private JsonNode readUntilResponse(
            int requestId,
            BlockingQueue<String> stdout,
            Process process,
            BufferedWriter writer,
            Instant deadline,
            StringBuilder streamedAnswer,
            StringBuilder completedAnswer,
            StringBuilder stderr
    ) {
        while (true) {
            JsonNode message = nextMessage(stdout, process, deadline, stderr);
            processNotificationOrServerRequest(message, writer, streamedAnswer, completedAnswer);
            JsonNode idNode = message.get("id");
            if (idNode != null && idNode.canConvertToInt() && idNode.asInt() == requestId) {
                if (message.has("error")) {
                    throw new CodexExecutionException("Codex app-server request failed: " + message.get("error"));
                }
                return message;
            }
        }
    }

    private JsonNode readUntilTurnCompleted(
            BlockingQueue<String> stdout,
            Process process,
            BufferedWriter writer,
            Instant deadline,
            StringBuilder streamedAnswer,
            StringBuilder completedAnswer,
            StringBuilder stderr
    ) {
        while (true) {
            JsonNode message = nextMessage(stdout, process, deadline, stderr);
            processNotificationOrServerRequest(message, writer, streamedAnswer, completedAnswer);
            JsonNode method = message.get("method");
            if (method != null && "turn/completed".equals(method.asText())) {
                return message;
            }
        }
    }

    private void processNotificationOrServerRequest(
            JsonNode message,
            BufferedWriter writer,
            StringBuilder streamedAnswer,
            StringBuilder completedAnswer
    ) {
        JsonNode methodNode = message.get("method");
        if (methodNode == null) {
            return;
        }
        String method = methodNode.asText();

        if ("item/agentMessage/delta".equals(method)) {
            JsonNode delta = message.path("params").path("delta");
            if (delta.isTextual()) {
                streamedAnswer.append(delta.asText());
            }
        } else if ("item/completed".equals(method)) {
            JsonNode item = message.path("params").path("item");
            if ("agentMessage".equals(item.path("type").asText()) && item.path("text").isTextual()) {
                completedAnswer.setLength(0);
                completedAnswer.append(item.path("text").asText());
            }
        }

        if (message.has("id")) {
            ObjectNode response = objectMapper.createObjectNode();
            response.set("id", message.get("id"));
            ObjectNode result = response.putObject("result");
            result.put("decision", "decline");
            try {
                send(writer, response);
            } catch (IOException ex) {
                throw new CodexExecutionException("Failed to decline unexpected Codex server request: " + method, ex);
            }
        }
    }

    private JsonNode nextMessage(
            BlockingQueue<String> stdout,
            Process process,
            Instant deadline,
            StringBuilder stderr
    ) {
        long remainingMillis = Duration.between(Instant.now(), deadline).toMillis();
        if (remainingMillis <= 0) {
            throw timeout(stderr);
        }
        try {
            String line = stdout.poll(remainingMillis, TimeUnit.MILLISECONDS);
            if (line == null) {
                throw timeout(stderr);
            }
            if (EOF.equals(line)) {
                throw new CodexExecutionException(
                        "Codex app-server exited before the request completed. exit=" + safeExitValue(process)
                                + stderrSuffix(stderr)
                );
            }
            return objectMapper.readTree(line);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CodexExecutionException("Interrupted while waiting for Codex app-server", ex);
        } catch (RuntimeException ex) {
            if (ex instanceof CodexExecutionException) {
                throw ex;
            }
            throw new CodexExecutionException("Invalid JSON received from Codex app-server", ex);
        }
    }

    private void validateTurnCompleted(JsonNode message, StringBuilder stderr) {
        JsonNode turn = message.path("params").path("turn");
        String status = turn.path("status").asText();
        if (!"completed".equals(status)) {
            JsonNode error = turn.path("error");
            throw new CodexExecutionException(
                    "Codex turn ended with status=" + status + (error.isMissingNode() || error.isNull() ? "" : ", error=" + error)
                            + stderrSuffix(stderr)
            );
        }
    }

    private String requiredText(JsonNode node, String... path) {
        JsonNode current = node;
        for (String segment : path) {
            current = current.path(segment);
        }
        if (!current.isTextual() || current.asText().isBlank()) {
            throw new CodexExecutionException("Codex app-server response did not contain required field: " + String.join(".", path));
        }
        return current.asText();
    }

    private Thread startStdoutReader(Process process, BlockingQueue<String> stdout) {
        return Thread.ofVirtual().name("codex-app-server-stdout").start(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.put(line);
                }
            } catch (IOException ex) {
                stdout.offer(EOF);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                stdout.offer(EOF);
            }
        });
    }

    private Thread startStderrReader(Process process, StringBuilder stderr) {
        return Thread.ofVirtual().name("codex-app-server-stderr").start(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (stderr) {
                        if (stderr.length() < 16_384) {
                            stderr.append(line).append('\n');
                        }
                    }
                }
            } catch (IOException ignored) {
                // Process shutdown can close stderr while the reader is blocked.
            }
        });
    }

    private CodexExecutionException timeout(StringBuilder stderr) {
        return new CodexExecutionException("Timed out waiting for Codex app-server" + stderrSuffix(stderr));
    }

    private String stderrSuffix(StringBuilder stderr) {
        synchronized (stderr) {
            String value = stderr.toString().trim();
            return value.isBlank() ? "" : ". stderr=" + value;
        }
    }

    private String safeExitValue(Process process) {
        try {
            return Integer.toString(process.exitValue());
        } catch (IllegalThreadStateException ex) {
            return "running";
        }
    }

    private void stopProcess(Process process) {
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    public record TurnResult(String threadId, String message) {
    }
}

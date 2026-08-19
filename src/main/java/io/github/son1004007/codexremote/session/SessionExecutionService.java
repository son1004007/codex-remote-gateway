package io.github.son1004007.codexremote.session;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class SessionExecutionService {

    private final AgentSessionPort sessions;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, ExecutionState> executions = new ConcurrentHashMap<>();

    public SessionExecutionService(AgentSessionPort sessions) {
        this.sessions = sessions;
    }

    public ExecutionSnapshot submit(String sessionId, String input) {
        sessions.get(sessionId);

        ExecutionState state;
        synchronized (executions) {
            ExecutionState current = executions.get(sessionId);
            if (current != null && current.isRunning()) {
                throw new IllegalStateException("A Codex turn is already running for session: " + sessionId);
            }
            state = new ExecutionState(sessionId);
            executions.put(sessionId, state);
        }

        Future<?> future = executor.submit(() -> executeTurn(state, input));
        state.setFuture(future);
        if (state.cancelRequested()) {
            boolean cancelled = future.cancel(true);
            if (cancelled && !state.started()) {
                cancelSessionBestEffort(sessionId);
                state.markCancelled();
            }
        }
        return state.snapshot();
    }

    public ExecutionSnapshot get(String sessionId) {
        sessions.get(sessionId);
        ExecutionState state = executions.get(sessionId);
        return state == null ? ExecutionSnapshot.idle(sessionId) : state.snapshot();
    }

    public ExecutionSnapshot cancel(String sessionId) {
        AgentSession session = sessions.get(sessionId);
        ExecutionState state = executions.get(sessionId);
        if (state != null && state.isRunning()) {
            state.requestCancel();
            Future<?> future = state.future();
            if (future != null) {
                boolean cancelled = future.cancel(true);
                if (cancelled && !state.started()) {
                    cancelSessionBestEffort(sessionId);
                    state.markCancelled();
                }
            }
            return state.snapshot();
        }

        if (session.status() == SessionStatus.ACTIVE) {
            sessions.cancel(sessionId);
        }
        ExecutionState cancelled = ExecutionState.cancelled(sessionId);
        executions.put(sessionId, cancelled);
        return cancelled.snapshot();
    }

    private void executeTurn(ExecutionState state, String input) {
        state.markStarted();
        try {
            if (state.cancelRequested()) {
                return;
            }
            sessions.submit(state.sessionId(), input);
            if (state.cancelRequested()) {
                state.markCancelled();
            } else {
                state.markSucceeded();
            }
        } catch (RuntimeException ex) {
            if (state.cancelRequested() || Thread.currentThread().isInterrupted()) {
                state.markCancelled();
            } else {
                state.markFailed(ex.getMessage());
            }
        } finally {
            if (state.cancelRequested()) {
                cancelSessionBestEffort(state.sessionId());
                state.markCancelled();
            }
        }
    }

    private void cancelSessionBestEffort(String sessionId) {
        try {
            AgentSession session = sessions.get(sessionId);
            if (session.status() == SessionStatus.ACTIVE) {
                sessions.cancel(sessionId);
            }
        } catch (RuntimeException ignored) {
            // Cancellation is best effort; provider/session errors remain visible through state/events.
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public enum ExecutionStatus {
        IDLE,
        RUNNING,
        CANCEL_REQUESTED,
        SUCCEEDED,
        FAILED,
        CANCELLED
    }

    public record ExecutionSnapshot(
            String sessionId,
            ExecutionStatus status,
            boolean running,
            Instant startedAt,
            Instant finishedAt,
            String error
    ) {
        static ExecutionSnapshot idle(String sessionId) {
            return new ExecutionSnapshot(sessionId, ExecutionStatus.IDLE, false, null, null, null);
        }
    }

    private static final class ExecutionState {
        private final String sessionId;
        private final Instant startedAt;
        private volatile ExecutionStatus status;
        private volatile Instant finishedAt;
        private volatile String error;
        private volatile Future<?> future;
        private volatile boolean cancelRequested;
        private volatile boolean started;

        private ExecutionState(String sessionId) {
            this.sessionId = sessionId;
            this.startedAt = Instant.now();
            this.status = ExecutionStatus.RUNNING;
        }

        private ExecutionState(String sessionId, ExecutionStatus status) {
            this.sessionId = sessionId;
            this.startedAt = Instant.now();
            this.status = status;
            this.finishedAt = this.startedAt;
            this.started = true;
        }

        static ExecutionState cancelled(String sessionId) {
            return new ExecutionState(sessionId, ExecutionStatus.CANCELLED);
        }

        String sessionId() {
            return sessionId;
        }

        synchronized boolean isRunning() {
            return status == ExecutionStatus.RUNNING || status == ExecutionStatus.CANCEL_REQUESTED;
        }

        synchronized void requestCancel() {
            cancelRequested = true;
            if (status == ExecutionStatus.RUNNING) {
                status = ExecutionStatus.CANCEL_REQUESTED;
            }
        }

        boolean cancelRequested() {
            return cancelRequested;
        }

        boolean started() {
            return started;
        }

        void markStarted() {
            started = true;
        }

        Future<?> future() {
            return future;
        }

        void setFuture(Future<?> future) {
            this.future = future;
        }

        synchronized void markSucceeded() {
            status = ExecutionStatus.SUCCEEDED;
            finishedAt = Instant.now();
            error = null;
        }

        synchronized void markFailed(String message) {
            status = ExecutionStatus.FAILED;
            finishedAt = Instant.now();
            error = message == null || message.isBlank() ? "Codex execution failed" : message;
        }

        synchronized void markCancelled() {
            status = ExecutionStatus.CANCELLED;
            finishedAt = Instant.now();
        }

        synchronized ExecutionSnapshot snapshot() {
            return new ExecutionSnapshot(
                    sessionId,
                    status,
                    isRunning(),
                    startedAt,
                    finishedAt,
                    error
            );
        }
    }
}

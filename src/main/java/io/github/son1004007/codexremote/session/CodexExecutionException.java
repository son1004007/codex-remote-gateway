package io.github.son1004007.codexremote.session;

public class CodexExecutionException extends RuntimeException {

    public CodexExecutionException(String message) {
        super(message);
    }

    public CodexExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}

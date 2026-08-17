package io.github.son1004007.codexremote.workflow;

final class WorkflowResultParser {

    private static final String RESULT_PREFIX = "WORKFLOW_RESULT:";

    private WorkflowResultParser() {
    }

    static WorkflowWorkerPort.Outcome parse(String message) {
        if (message == null || message.isBlank()) {
            return WorkflowWorkerPort.Outcome.BLOCKED;
        }

        String marker = message.lines()
                .map(String::strip)
                .filter(line -> line.startsWith(RESULT_PREFIX))
                .reduce((first, second) -> second)
                .orElse("");

        String value = marker.substring(Math.min(marker.length(), RESULT_PREFIX.length())).strip();
        if (value.startsWith("SUCCESS")) {
            return WorkflowWorkerPort.Outcome.SUCCESS;
        }
        if (value.startsWith("FAILED")) {
            return WorkflowWorkerPort.Outcome.FAILED;
        }
        return WorkflowWorkerPort.Outcome.BLOCKED;
    }
}

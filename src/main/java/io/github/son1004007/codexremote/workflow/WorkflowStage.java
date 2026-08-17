package io.github.son1004007.codexremote.workflow;

public enum WorkflowStage {
    PLAN,
    IMPLEMENT,
    TEST,
    REVIEW,
    DEPLOY,
    E2E;

    public WorkflowStage next() {
        return switch (this) {
            case PLAN -> IMPLEMENT;
            case IMPLEMENT -> TEST;
            case TEST -> REVIEW;
            case REVIEW -> DEPLOY;
            case DEPLOY -> E2E;
            case E2E -> null;
        };
    }
}

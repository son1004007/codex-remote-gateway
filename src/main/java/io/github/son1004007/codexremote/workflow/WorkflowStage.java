package io.github.son1004007.codexremote.workflow;

public enum WorkflowStage {
    PLAN,
    PLAN_VERIFY,
    IMPLEMENT,
    TEST_DESIGN,
    TEST,
    REVIEW,
    REVIEW_VERIFY,
    DEPLOY,
    E2E;

    public WorkflowStage next() {
        return switch (this) {
            case PLAN -> PLAN_VERIFY;
            case PLAN_VERIFY -> IMPLEMENT;
            case IMPLEMENT -> TEST_DESIGN;
            case TEST_DESIGN -> TEST;
            case TEST -> REVIEW;
            case REVIEW -> REVIEW_VERIFY;
            case REVIEW_VERIFY -> DEPLOY;
            case DEPLOY -> E2E;
            case E2E -> null;
        };
    }
}

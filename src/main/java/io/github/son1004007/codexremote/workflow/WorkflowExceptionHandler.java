package io.github.son1004007.codexremote.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WorkflowExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    ProblemDetail handleNotFound(WorkflowNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Workflow not found");
        return detail;
    }

    @ExceptionHandler({WorkflowStateException.class, WorkflowWorkspaceConflictException.class})
    ProblemDetail handleConflict(RuntimeException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("Invalid workflow state");
        return detail;
    }
}

package io.github.son1004007.codexremote.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflows;

    public WorkflowController(WorkflowService workflows) {
        this.workflows = workflows;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WorkflowSnapshot create(@Valid @RequestBody CreateWorkflowRequest request) {
        return workflows.create(request.workspaceId(), request.goal(), request.autoDeploy());
    }

    @GetMapping
    public List<WorkflowSnapshot> list() {
        return workflows.list();
    }

    @GetMapping("/{workflowId}")
    public WorkflowSnapshot get(@PathVariable String workflowId) {
        return workflows.get(workflowId);
    }

    @PostMapping("/{workflowId}/approve")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WorkflowSnapshot approve(@PathVariable String workflowId) {
        return workflows.approve(workflowId);
    }

    @PostMapping("/{workflowId}/resume")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WorkflowSnapshot resume(@PathVariable String workflowId) {
        return workflows.resume(workflowId);
    }

    @PostMapping("/{workflowId}/cancel")
    public WorkflowSnapshot cancel(@PathVariable String workflowId) {
        return workflows.cancel(workflowId);
    }

    public record CreateWorkflowRequest(
            @NotBlank String workspaceId,
            @NotBlank @Size(max = 20_000) String goal,
            boolean autoDeploy
    ) {
    }
}

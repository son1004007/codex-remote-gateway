# Verified Workflow Evidence — v1

This slice separates **agent self-report** from **deterministic repository evidence**.

## Create a workflow with explicit acceptance criteria

```http
POST /api/v1/workflows
Content-Type: application/json

{
  "workspaceId": "my-repo",
  "goal": "Implement the requested change safely",
  "acceptanceCriteria": [
    "Repository tests pass",
    "No unrelated files are changed"
  ],
  "autoDeploy": false
}
```

Acceptance criteria are stored in `WorkflowSnapshot` and included in every worker-stage instruction.

## Run a deterministic verifier

When the workflow is in a stable state (`WAITING_APPROVAL`, `BLOCKED`, `FAILED`, or `COMPLETED`):

```http
POST /api/v1/workflows/{workflowId}/verify
Content-Type: application/json

{
  "profile": "MAVEN_TEST"
}
```

Supported bounded profiles:

- `MAVEN_TEST`
- `MAVEN_VERIFY`
- `GRADLE_TEST`
- `PYTEST`
- `NPM_TEST`
- `GIT_DIFF_CHECK`

The gateway executes a fixed argument vector with `ProcessBuilder`; the request cannot supply an arbitrary shell command.

A verifier result records:

- selected profile
- exact command vector
- exit code
- success flag
- bounded output
- truncation flag
- start/completion timestamps

This result is intentionally separate from model `WORKFLOW_RESULT` text.

## Retrieve an evidence bundle

```http
GET /api/v1/workflows/{workflowId}/evidence
```

The bundle contains:

- full workflow snapshot
- explicit acceptance criteria
- stage workers and stage outputs (model-reported evidence)
- latest deterministic verifier result
- Git status
- unstaged diff
- staged diff
- workflow events including approval events
- generation timestamp

## Security boundaries

- Verification profiles are allow-listed; no user-supplied shell is executed.
- Existing workspace-root and symlink protections remain authoritative.
- Verification is rejected while the workflow is not in a stable state.
- Git evidence uses the existing bounded `WorkspaceService` execution and output limits.
- Verification output is capped and timed out.

## Current limitation

The latest deterministic verification result is in-memory. Durable persistence, SSE/event streaming, evidence artifact export, and automatic verification gating remain follow-up work under Issue #9.

OpenClaw remains a later second-runtime adapter. Complete the evidence/verification slice before Issue #10.

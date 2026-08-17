# Workflow Controller

## Purpose

The workflow controller reduces the human control surface for Linux-based AI development work. A client such as mobile ChatGPT should need only a small set of commands: create a workflow, inspect status, approve a guarded deployment, resume a blocked task, or cancel it.

The first implementation deliberately reuses the existing `AgentSessionPort` and Codex App Server integration instead of replacing the M1 session layer.

## API

Base path: `/api/v1/workflows`

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/workflows` | Create and asynchronously start a workflow |
| `GET` | `/api/v1/workflows` | List workflow state |
| `GET` | `/api/v1/workflows/{id}` | Get detailed workflow state and events |
| `POST` | `/api/v1/workflows/{id}/approve` | Approve a workflow waiting at the deploy gate |
| `POST` | `/api/v1/workflows/{id}/resume` | Retry the current stage after `BLOCKED` or `FAILED` |
| `POST` | `/api/v1/workflows/{id}/cancel` | Stop further workflow progression and release the worker session best-effort |

Example request:

```json
{
  "workspaceId": "project-a",
  "goal": "Implement the requested API change, test it, review it, deploy to the explicitly configured test target, and prove E2E behavior.",
  "autoDeploy": false
}
```

`autoDeploy` defaults to `false` when omitted. Keep it false for normal operation.

## State machine

```text
READY
  -> PLAN
  -> IMPLEMENT
  -> TEST
  -> REVIEW
  -> WAITING_APPROVAL (DEPLOY, default)
  -> DEPLOY
  -> E2E
  -> COMPLETED
```

A worker can stop a stage as `BLOCKED` or `FAILED`. `resume` retries the current stage. `cancel` prevents later stages.

Only one active workflow is allowed per workspace in this first slice. This matches the existing M1 session exclusivity and prevents two agents from mutating the same checkout concurrently.

## Human gates

Normal human intervention is intentionally limited to:

1. ambiguous or missing information reported as `BLOCKED`;
2. failed execution requiring a retry or changed instruction;
3. deployment approval when `autoDeploy=false`.

The controller does not treat a worker response as successful unless the response contains an explicit final marker:

```text
WORKFLOW_RESULT: SUCCESS
WORKFLOW_RESULT: BLOCKED - <reason>
WORKFLOW_RESULT: FAILED - <reason>
```

Missing markers fail closed as `BLOCKED` in the Codex adapter.

## Worker boundary

`WorkflowWorkerPort` is the provider boundary.

Current implementations:

- `InMemoryWorkflowWorkerAdapter`: deterministic development/test worker.
- `CodexWorkflowWorkerAdapter`: reuses `AgentSessionPort` and therefore the existing Codex App Server thread/session behavior.

Future routing should preserve the controller API and replace or wrap the worker port, for example:

```text
PLAN/REVIEW      -> Gemini or another reviewer/planner
IMPLEMENT/TEST   -> Codex
DEPLOY/E2E       -> controlled runner
```

Do not bind provider-specific code into `WorkflowController` or `WorkflowService`.

## Safety properties

- No deployment occurs before the `DEPLOY` stage.
- Deployment requires explicit approval by default.
- The worker prompt forbids invented credentials, targets, and test evidence.
- A missing structured result marker is not interpreted as success.
- Existing workspace path validation remains owned by the Codex session adapter.
- Cancellation and successful completion release the underlying M1 session best-effort so the workspace can be used again.
- Workflow state is currently in memory and is lost on gateway restart.

## Known limitations

This is the minimum controller slice, not the final workflow engine.

- Workflow/task state is not persisted yet.
- There is no GitHub issue/PR state adapter yet.
- There is no Gemini/Antigravity worker implementation yet.
- Codex currently performs planning, implementation, testing, and review when `gateway.agent.mode=codex`; review is therefore role-separated but not provider-independent.
- Existing Codex cancellation still cannot interrupt a turn already executing inside the M1 adapter.
- Production authentication is not added by this change; keep the HTTP service loopback/private-network protected.
- Concurrent workflows for the same workspace remain intentionally disabled.

## Recommended next slice

1. persist workflow snapshots and events in PostgreSQL;
2. add a GitHub task adapter for Issue/PR/check state;
3. add stage-aware worker routing with Codex for implementation and Gemini/Antigravity for review/planning;
4. add a controlled deployment runner instead of letting an AI worker directly own deployment mechanics;
5. expose a narrow ChatGPT-facing control contract: `create`, `status`, `approve`, `resume`, `cancel`.

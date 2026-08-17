# Codex Remote Gateway

Self-hosted control plane and multi-provider AI gateway for running and supervising AI-assisted development workflows on Linux.

## Goal

The project provides a remote control plane for development agents running on Linux. The intended human control surface is small enough for mobile operation: start work, inspect status, approve guarded actions, resume blocked work, or cancel it.

The workflow does not trust one AI to plan, implement, review, and certify its own result. Codex and Gemini/Antigravity are assigned different jobs and the claims of one are verified by the other or by executable evidence.

## Current implementation

```text
Mobile ChatGPT / API client
  -> WorkflowService
  -> RoutingWorkflowWorkerAdapter
       |
       |-- PLAN ----------> Antigravity/Gemini (optional)
       |-- PLAN_VERIFY ---> Codex
       |-- IMPLEMENT -----> Codex
       |-- TEST_DESIGN ---> Antigravity/Gemini (optional)
       |-- TEST ----------> Codex + real tools
       |-- REVIEW --------> Antigravity/Gemini (optional)
       |-- REVIEW_VERIFY -> Codex + reproduction/tests
       |-- DEPLOY --------> Codex for now, after human gate
       `-- E2E -----------> Codex for now
```

When Antigravity is disabled, its preferred stages fall back to Codex so existing deployments remain compatible.

Implemented:

- Spring Boot 4.1 / Java 21 backend.
- Session create/list/get, prompt submission, and cancel APIs.
- Workflow create/list/get, approve, resume, and cancel APIs under `/api/v1/workflows`.
- Asynchronous sequential workflow execution on Java 21 virtual threads.
- Verified workflow stages: `PLAN -> PLAN_VERIFY -> IMPLEMENT -> TEST_DESIGN -> TEST -> REVIEW -> REVIEW_VERIFY -> DEPLOY -> E2E`.
- Explicit per-stage worker and output evidence for cross-provider handoff.
- Human deployment approval by default; `autoDeploy=true` must be explicit.
- Codex as the real-workspace implementation, execution, and verification worker.
- Optional Antigravity/Gemini worker for broad planning, adversarial test design, and independent review.
- Antigravity runs against a disposable analysis snapshot plus bounded read-only Git context, so it does not mutate the real workspace.
- Fail-closed workflow result parsing: a missing `WORKFLOW_RESULT` marker becomes `BLOCKED`, not success.
- One active workflow per workspace to prevent uncontrolled concurrent mutation.
- Codex App Server initialize, thread start/resume, turn start, event collection, and turn completion flow.
- Workspace root enforcement including parent traversal, absolute-path, and symlink-escape protection.
- Problem-detail error responses and Actuator health endpoint.
- Red Hat UBI 9 + Node.js 22 + Java 21 + Codex CLI container image.
- Docker Compose deployment configuration bound to loopback by default.
- Persistent Codex home and mounted workspace support.
- Headless ChatGPT device-login helper.
- Deployment preflight, health polling, remote SSH helper, and two-turn Codex smoke test.
- GitHub Actions Java, shell/Compose, and container-image verification.

Not implemented yet:

- browser/frontend;
- gateway HTTP authentication/authorization;
- SSE/WebSocket streaming;
- persistent workflow/session state;
- GitHub Issue/PR/check state integration;
- Git status/diff UI;
- Antigravity installation/authentication inside the current UBI image;
- controlled deployment/E2E runner separated from AI workers;
- local GPU provider;
- bounded-parallel or DAG-style multi-agent orchestration.

Because HTTP authentication is not implemented yet, the test deployment binds to `127.0.0.1` by default. Do not expose it directly to the public Internet.

## Enable Codex + Antigravity collaboration

Antigravity collaboration is off by default. Install and authenticate `agy` on the Linux runtime first, then configure:

```text
GATEWAY_AGENT_MODE=codex
GATEWAY_ANTIGRAVITY_ENABLED=true
GATEWAY_ANTIGRAVITY_COMMAND=agy
GATEWAY_ANTIGRAVITY_MODEL=
GATEWAY_ANTIGRAVITY_TURN_TIMEOUT=5m
```

Leave the model blank until the desired Flash model has been verified on that host. The CLI-configured/default model will be used when the value is blank.

See `docs/WORKFLOW-CONTROLLER.md` for the role policy, evidence handoff, safety boundary, state machine, and limitations.

## Workflow control example

Create a workflow:

```bash
curl -sS -X POST http://127.0.0.1:18080/api/v1/workflows \
  -H 'Content-Type: application/json' \
  -d '{
    "workspaceId": "project-a",
    "goal": "Implement the requested change, test it, review it, then deploy and verify E2E.",
    "autoDeploy": false
  }'
```

Inspect status:

```bash
curl -sS http://127.0.0.1:18080/api/v1/workflows/<workflow-id>
```

The response includes `stageWorkers` and `stageOutputs`, making it possible for a mobile supervisor to see which model produced each material claim without opening the underlying CLI sessions.

Approve deployment only when the workflow reaches `WAITING_APPROVAL` at `DEPLOY`:

```bash
curl -sS -X POST http://127.0.0.1:18080/api/v1/workflows/<workflow-id>/approve
```

## Server test quick start

```bash
git clone https://github.com/son1004007/codex-remote-gateway.git
cd codex-remote-gateway
cp .env.example .env
bash scripts/preflight.sh
bash scripts/codex-login.sh
bash scripts/deploy-local.sh
bash scripts/smoke-test.sh
```

The existing smoke test verifies health, session creation, a real Codex turn, an assistant response, and a second turn that resumes the same Codex thread. Real private-server collaborative workflow E2E validation remains a separate acceptance step until `agy` is installed/authenticated and actual workflow evidence is captured.

For deployment details, SELinux notes, rollback, acceptance criteria, and the generic SSH deployment helper, see `docs/DEPLOYMENT.md`.

## Design principles

1. Control plane and execution plane remain separate.
2. Provider selection belongs behind explicit stage routing.
3. Plans are hypotheses until independently verified against the repository.
4. Review findings are hypotheses until reproduced or otherwise verified.
5. Risky actions pass explicit human approval gates unless a safer automation policy was deliberately enabled.
6. Git and executable tests remain evidence ledgers; AI assertions are not evidence by themselves.
7. Read-only analysis agents do not receive the mutable real workspace.
8. Delivery proceeds from one workspace/session to verified sequential orchestration and only then to bounded parallel multi-agent execution.

## License

Licensed under the Apache License, Version 2.0. See `LICENSE` and `NOTICE`.

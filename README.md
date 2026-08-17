# Codex Remote Gateway

Self-hosted control plane and multi-provider AI gateway for running and supervising AI-assisted development workflows on Linux.

## Goal

The project provides a remote control plane for development agents running on Linux. Codex is the first coding-agent integration. Provider selection and future local inference remain separate concerns so Gemini/Antigravity, Groq, local models, and other providers can be added without coupling the control plane to one vendor.

The intended human control surface is small enough for mobile operation: start work, inspect status, approve guarded actions, resume blocked work, or cancel it.

## Current implementation

The original M1 session layer remains intact and a bounded workflow-controller slice now sits above it:

```text
REST API
  -> WorkflowService
  -> WorkflowWorkerPort
  -> CodexWorkflowWorkerAdapter
  -> AgentSessionPort
  -> CodexAgentSessionAdapter
  -> Codex App Server (JSONL/stdin/stdout)
  -> ChatGPT-authenticated Codex runtime
  -> mounted workspace
```

Implemented:

- Spring Boot 4.1 / Java 21 backend.
- Session create/list/get, prompt submission, and cancel APIs.
- Workflow create/list/get, approve, resume, and cancel APIs under `/api/v1/workflows`.
- Asynchronous sequential workflow execution on Java 21 virtual threads.
- Workflow stages: `PLAN -> IMPLEMENT -> TEST -> REVIEW -> DEPLOY -> E2E`.
- Human deployment approval by default; `autoDeploy=true` must be explicit.
- Provider-neutral `WorkflowWorkerPort` with in-memory and Codex implementations.
- Fail-closed Codex workflow result parsing: a missing `WORKFLOW_RESULT` marker becomes `BLOCKED`, not success.
- One active workflow per workspace to prevent uncontrolled concurrent mutation.
- Selectable in-memory and Codex-backed session adapters.
- Codex App Server initialize, thread start/resume, turn start, event collection, and turn completion flow.
- Workspace root enforcement including parent traversal, absolute-path, and symlink-escape protection.
- Problem-detail error responses.
- Actuator health endpoint.
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
- active-turn interruption;
- persistent workflow/session state;
- GitHub Issue/PR/check state integration;
- Git status/diff UI;
- Gemini/Antigravity workflow worker and stage-aware provider routing;
- controlled deployment runner separated from the AI worker;
- local GPU provider;
- bounded-parallel or DAG-style multi-agent orchestration.

Because HTTP authentication is not implemented yet, the test deployment binds to `127.0.0.1` by default. Do not expose it directly to the public Internet.

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

Approve deployment only when the workflow reaches `WAITING_APPROVAL` at `DEPLOY`:

```bash
curl -sS -X POST http://127.0.0.1:18080/api/v1/workflows/<workflow-id>/approve
```

See `docs/WORKFLOW-CONTROLLER.md` for the state machine, safety behavior, worker contract, and limitations.

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

The existing smoke test verifies health, session creation, a real Codex turn, an assistant response, and a second turn that resumes the same Codex thread. Real private-server workflow-controller E2E validation is still a separate acceptance step and is not claimed by this README.

For deployment details, SELinux notes, rollback, acceptance criteria, and the generic SSH deployment helper, see `docs/DEPLOYMENT.md`.

## Architecture direction

```text
Mobile ChatGPT / Browser
  |
  v
Workflow / Web Control Plane
  |-- workflow status
  |-- guarded approvals
  |-- session management
  |-- streaming output/events (planned)
  |-- Git status/diff (planned)
  |
  v
WorkflowWorkerPort
  |-- Codex implementation/testing
  |-- Gemini/Antigravity planning/review (planned)
  |-- controlled runner deployment/E2E (planned)
  |
  v
Workspace / Runner
```

See `docs/ARCHITECTURE.md`, `docs/WORKFLOW-CONTROLLER.md`, and `docs/ROADMAP.md` for the broader design.

## Design principles

1. Control plane and execution plane remain separate.
2. Provider selection belongs behind explicit adapters/gateways.
3. Risky actions pass explicit human approval gates unless a safer automation policy was deliberately enabled.
4. Git remains the primary change ledger.
5. Workspace isolation and least privilege take priority over exposing a convenient shell.
6. Execution state and failures should be observable and reproducible.
7. Delivery proceeds from one workspace/session to sequential orchestration and only then to bounded parallel multi-agent execution.

## License

Licensed under the Apache License, Version 2.0. See `LICENSE` and `NOTICE`.

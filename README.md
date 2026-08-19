# Codex Remote Gateway

Self-hosted Web UI and control gateway for operating the Codex instance and development workspaces on a Linux server from a PC or mobile browser.

## Goal

The primary product is **per-server Codex control**. Each Linux server runs its own `codex-remote-gateway`, Codex runtime, and local workspace boundary. A browser connects directly to that server's gateway; a mandatory central controller is not required.

GitHub remains the durable source/change/evidence ledger and can be used for handoff between ChatGPT planning/review and Codex implementation. The interactive Web UI is the direct control surface for the Codex instance on the selected server.

The repository also contains optional higher-level Codex + Gemini/Antigravity workflow orchestration. Those capabilities must not make the basic per-server GUI dependent on a central service.

## Current browser-control slice

PR #3 adds the first direct browser-control vertical slice:

- responsive PC/mobile UI at `/`;
- safe direct-child workspace discovery;
- session create/list/select/reconnect;
- asynchronous Codex prompt submission with execution-state polling;
- session event timeline;
- best-effort stop/cancel;
- read-only Git status, working-tree diff, and staged diff;
- browser-aware real-Codex smoke coverage.

The current UI uses polling rather than SSE/WebSocket incremental streaming. Session metadata is still in memory and is lost when the gateway restarts. Git controls are intentionally read-only in this slice.

**Authentication is not implemented yet. Keep the gateway on loopback/private networking. Do not expose the Codex UI or API directly to the public Internet.** Authenticated HTTPS ingress is the next network-facing slice after browser control is validated on both selected Linux targets.

## Per-server topology

```text
PC / Mobile browser
        |
        +----> Linux server A
        |        codex-remote-gateway
        |          -> local Codex
        |          -> local workspaces
        |
        `----> Linux server B
                 codex-remote-gateway
                   -> local Codex
                   -> local workspaces

GitHub
  = durable source/change/evidence handoff
```

The first private runtime targets are the existing primary IDC Linux server and the existing RTX2080 Linux server recorded in the private device inventory. Private host identifiers and credentials are not stored in this public repository.

## Existing workflow implementation

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

Implemented before the browser slice:

- Spring Boot 4.1 / Java 21 backend.
- Session create/list/get, prompt submission, and cancel APIs.
- Workflow create/list/get, approve, resume, and cancel APIs under `/api/v1/workflows`.
- Asynchronous sequential workflow execution on Java 21 virtual threads.
- Verified workflow stages: `PLAN -> PLAN_VERIFY -> IMPLEMENT -> TEST_DESIGN -> TEST -> REVIEW -> REVIEW_VERIFY -> DEPLOY -> E2E`.
- Explicit per-stage worker and output evidence for cross-provider handoff.
- Human deployment approval by default; `autoDeploy=true` must be explicit.
- Codex as the real-workspace implementation, execution, and verification worker.
- Optional Antigravity/Gemini worker for broad planning, adversarial test design, and independent review.
- Antigravity disposable analysis snapshots plus bounded read-only Git context.
- Fail-closed workflow result parsing.
- One active workflow per workspace.
- Codex App Server initialize, thread start/resume, turn start, event collection, and turn completion flow.
- Workspace root enforcement including traversal and symlink-escape protection.
- Problem-detail error responses and Actuator health endpoint.
- Red Hat UBI 9 + Node.js 22 + Java 21 + pinned Codex CLI container image.
- Docker Compose deployment configuration bound to loopback by default.
- Persistent Codex home and mounted workspace support.
- Headless ChatGPT device-login helper.
- Deployment preflight, health polling, remote SSH deployment, and real Codex smoke tests.
- GitHub Actions Java, shell/Compose, and container-image verification.

Still to implement/validate:

- authenticated Web access and authorization;
- SSE/WebSocket incremental streaming;
- persisted gateway session/thread metadata and restart recovery;
- guarded Git commit/push operations if they are later exposed in the UI;
- GitHub Issue/PR/check state integration;
- Antigravity installation/authentication inside the current UBI image;
- controlled deployment/E2E runner separated from AI workers;
- local GPU provider;
- bounded-parallel or DAG-style multi-agent orchestration.

## Direct browser usage

After a private/local deployment succeeds, open the gateway endpoint from an allowed browser path. The default Compose bind is intentionally loopback:

```text
http://127.0.0.1:18080/
```

The browser lets you select an available workspace, create or reconnect to an active session, submit a Codex request, monitor execution state and events, stop the session best-effort, and inspect read-only Git state/diffs.

Do not change `GATEWAY_BIND_ADDRESS` to a broadly reachable interface until authentication and HTTPS ingress are in place.

## Enable Codex + Antigravity collaboration

Antigravity collaboration is off by default. Install and authenticate `agy` on the Linux runtime first, then configure:

```text
GATEWAY_AGENT_MODE=codex
GATEWAY_ANTIGRAVITY_ENABLED=true
GATEWAY_ANTIGRAVITY_COMMAND=agy
GATEWAY_ANTIGRAVITY_MODEL=
GATEWAY_ANTIGRAVITY_TURN_TIMEOUT=5m
```

Leave the model blank until the desired model has been verified on that host. The CLI-configured/default model will be used when the value is blank.

See `docs/WORKFLOW-CONTROLLER.md` for the optional orchestration role policy, evidence handoff, safety boundary, state machine, and limitations.

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

The browser-control smoke verifies health, the root Web UI, session creation, a real asynchronous Codex turn, an assistant response, and a second turn that resumes the same Codex thread.

For deployment details, SELinux notes, rollback, acceptance criteria, and the generic SSH deployment helper, see `docs/DEPLOYMENT.md`.

## Design principles

1. Each Linux server can operate its local Codex independently.
2. The Web UI is a controlled Codex interface, not an unrestricted browser shell.
3. Git remains the primary source-change evidence ledger.
4. Authentication precedes broad/public network exposure.
5. Plans are hypotheses until independently verified against the repository.
6. Review findings are hypotheses until reproduced or otherwise verified.
7. Risky actions pass explicit human approval gates unless a safer automation policy was deliberately enabled.
8. Multi-agent orchestration remains optional and post-MVP relative to direct per-server Codex control.

## License

Licensed under the Apache License, Version 2.0. See `LICENSE` and `NOTICE`.

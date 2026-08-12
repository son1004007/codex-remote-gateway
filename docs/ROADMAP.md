# Roadmap

The implementation order deliberately reduces uncertainty before adding multi-agent complexity.

## M0 - Repository and decisions

- [x] public repository
- [x] Apache-2.0 license
- [x] product scope
- [x] initial architecture
- [x] security baseline
- [x] AI gateway concept
- [ ] architecture decision records for backend/frontend/transport
- [ ] supported Codex integration contract verified against current official interface

Exit condition: implementation boundaries and unsupported assumptions are explicit.

## M1 - Single workspace, single Codex session

- [ ] Spring Boot control-plane skeleton
- [ ] health/readiness endpoints
- [ ] local authentication for development
- [ ] workspace configuration/registry
- [ ] Codex adapter interface
- [ ] start session
- [ ] submit input
- [ ] receive/normalize events
- [ ] cancel/close session
- [ ] persist session metadata

Exit condition: one registered repository can be operated through an API without browser terminal access.

## M2 - Web control plane

- [ ] web UI skeleton
- [ ] login/session handling
- [ ] workspace list
- [ ] session list/detail
- [ ] prompt input
- [ ] WebSocket or SSE event stream
- [ ] reconnect after browser refresh/disconnect
- [ ] explicit error/status UI

Exit condition: normal Codex session operation works remotely through a browser.

## M3 - Approval and Git review

- [ ] approval request model
- [ ] approval policy categories
- [ ] approve/reject API
- [ ] approval UI
- [ ] Git status endpoint
- [ ] changed-file endpoint
- [ ] unified diff endpoint/view
- [ ] audit events

Exit condition: a user can review both risky operations and resulting source changes before proceeding.

## M4 - Runner isolation

- [ ] dedicated runner interface
- [ ] Docker runner implementation
- [ ] workspace mount policy
- [ ] non-root container user
- [ ] CPU/memory/PID/time limits
- [ ] outbound network policy
- [ ] secret injection policy
- [ ] cleanup/recovery behavior

Exit condition: normal project execution does not require granting the web service unrestricted host execution.

## M5 - Multi-provider AI gateway

- [ ] provider SPI
- [ ] normalized request/response model
- [ ] streaming abstraction
- [ ] OpenAI/Codex-compatible adapter
- [ ] Gemini adapter
- [ ] Groq adapter
- [ ] explicit provider/model selection
- [ ] retry/timeout/error normalization
- [ ] usage telemetry

Exit condition: application features can invoke supported model capabilities without vendor-specific controller/UI code.

## M6 - Operational hardening

- [ ] PostgreSQL migrations
- [ ] structured logging
- [ ] metrics
- [ ] backup/recovery procedure
- [ ] reverse-proxy/TLS deployment
- [ ] production authentication strategy
- [ ] secret management
- [ ] dependency/security scanning
- [ ] threat-model review

Exit condition: suitable for controlled long-running personal/team deployment.

## M7 - Agent workflow engine

- [ ] workflow/task schema
- [ ] dependency graph
- [ ] task state machine
- [ ] sequential execution
- [ ] bounded parallel execution
- [ ] isolated Git worktree/branch per concurrent coding task
- [ ] artifact handoff
- [ ] retry/resume
- [ ] human approval nodes
- [ ] workflow UI

Exit condition: multiple specialized agents can collaborate without sharing uncontrolled mutable state.

## Suggested first implementation slice

Implement only this vertical slice first:

```text
Browser
 -> Spring Boot API
 -> one configured workspace
 -> Codex adapter
 -> one persistent session
 -> streamed events
 -> Git diff
```

Do not implement Gemini/Groq routing or multi-agent DAG execution until this slice is stable. Those capabilities depend on a reliable session/event/control foundation.

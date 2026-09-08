# Open Questions

Items here are unresolved. Do not treat them as confirmed implementation requirements until a decision is recorded.

## OQ-001: Frontend stack

Status: `UNKNOWN`

Need to choose the frontend implementation approach for the web control plane.

Candidates may include a lightweight server-rendered approach or a separate SPA, but no choice is confirmed yet.

## OQ-002: Codex integration transport

Status: `RESOLVED`

Resolved by `D-013` and current implementation evidence.

The current Codex-backed `AgentSessionPort` uses `codex app-server` over JSONL stdio. `CURRENT_STATE.md` and regression tests describe the implemented initialize/thread/turn lifecycle.

Do not revert this item to `UNKNOWN` merely because other Codex embedding options such as Exec or SDK exist. Those remain possible **future adapters or bounded-use interfaces**, not evidence that the current transport is undecided.

Any future change to the primary transport requires a new explicit decision backed by current official documentation and executable migration/regression evidence.

## OQ-003: WebSocket vs SSE

Status: `UNKNOWN`

Need to decide whether live session/event transport uses WebSocket, SSE plus normal HTTP commands, or another pattern.

Decision criteria:

- reconnect behavior;
- bidirectional control needs;
- proxy compatibility;
- implementation complexity;
- backpressure/event volume.

## OQ-004: Authentication model

Status: `UNKNOWN`

Development authentication and production authentication are not yet selected.

Need to decide whether the first deployment is local/VPN-only, password-based, OAuth/OIDC, or another model.

## OQ-005: Persistence deployment

Status: `UNKNOWN`

PostgreSQL is the planned durable store, but the initial deployment location and lifecycle are not confirmed.

## OQ-006: Runner isolation level for M1

Status: `UNKNOWN`

Need to decide whether M1 begins with a dedicated non-root Linux user or containerized execution from the first implementation.

## OQ-007: AI framework choice inside Spring Boot

Status: `UNKNOWN`

Need to verify whether Spring AI should be used for provider adapters or whether direct SDK/HTTP adapters give better control for the gateway's capability model.

## OQ-008: Public Internet exposure

Status: `UNKNOWN`

The first deployment target and network exposure are not confirmed. Private/VPN access is preferred until authentication, authorization, rate limiting, and runner isolation are hardened.

# Current State

Last reviewed: 2026-08-13

## Repository status

- `CONFIRMED`: Public GitHub repository exists at `son1004007/codex-remote-gateway`.
- `CONFIRMED`: License is Apache License 2.0.
- `IMPLEMENTED`: Repository-level AI instructions and LLM Wiki exist.
- `IMPLEMENTED`: Spring Boot 4.1.0 / Java 21 backend exists.
- `IMPLEMENTED`: Basic Actuator health/info exposure exists.
- `IMPLEMENTED`: Provider-neutral `AgentSessionPort` exists.
- `IMPLEMENTED`: In-memory adapter exists for development/tests.
- `IMPLEMENTED`: Codex-backed adapter exists and communicates with `codex app-server` over JSONL stdio.
- `IMPLEMENTED`: Codex initialize, thread start/resume, turn start, streamed agent-message collection, and turn-completed handling exist.
- `IMPLEMENTED`: Gateway sessions retain the Codex provider thread ID in memory so the next request can resume the same thread.
- `IMPLEMENTED`: Workspace resolution is restricted to the configured workspace root and rejects parent traversal, absolute-path escape, and symlink escape.
- `IMPLEMENTED`: REST endpoints exist for session create/list/get, message submission, and cancellation.
- `IMPLEMENTED`: Validation and problem-detail responses exist for request/session/workspace/Codex errors.
- `IMPLEMENTED`: UBI 9 + Node.js 22 + Java 21 + pinned Codex CLI container definition exists.
- `IMPLEMENTED`: Docker Compose, preflight, runtime-volume preparation, device-login, deploy, remote-target, and two-turn smoke-test automation exists.
- `IMPLEMENTED`: GitHub Actions verifies Java tests, shell syntax, Compose configuration, UBI image build, and image-local Codex CLI version.

## Current product direction

- `CONFIRMED`: The product is a self-hosted web control plane for AI-assisted development workflows on Linux.
- `CONFIRMED`: Codex is the initial coding-agent target.
- `CONFIRMED`: The system should support web-based session control, event streaming, approval handling, and Git status/diff review.
- `CONFIRMED`: A Spring Boot AI Gateway is planned for provider-neutral access to multiple AI providers.
- `PLANNED`: Initial additional providers include Gemini, Groq, and a separately isolated local-model provider.
- `PLANNED`: Multi-agent workflow orchestration is a later phase, not current M1 scope.

## Current implementation milestone

Roadmap status: `M1 - Single workspace, single Codex session` is server-testable but not yet validated on the selected private infrastructure.

Current path:

```text
Spring Boot API
 -> AgentSessionPort
 -> CodexAgentSessionAdapter
 -> Codex App Server process per submitted turn
 -> ChatGPT-authenticated Codex state under persistent CODEX_HOME
 -> mounted workspace
```

Deployment path:

```text
Red Hat UBI 9 container
 -> host loopback port
 -> persistent Codex home bind mount
 -> configured workspace root bind mount
 -> device-authenticated Codex CLI
 -> two-turn smoke test
```

## Verification evidence

- `CONFIRMED`: Java regression tests include session lifecycle, API behavior, Codex App Server fake-protocol start/resume, and workspace-isolation cases.
- `CONFIRMED`: An ISTQB-oriented negative test reproduced a symlink workspace escape before the implementation was fixed.
- `CONFIRMED`: The symlink-escape regression test passes after real-path enforcement.
- `CONFIRMED`: An initial UBI container build exposed a `curl-minimal` versus `curl` package conflict; the redundant `curl` install was removed.
- `UNKNOWN`: Real ChatGPT-authenticated Codex turn behavior on the selected private servers remains unverified until the remote commands are executed there.

## Important limitations

- `CONFIRMED`: Gateway session state remains in memory and is lost when Spring Boot restarts.
- `CONFIRMED`: Codex thread files can persist in `CODEX_HOME`, but the gateway session-to-thread mapping is not persisted.
- `CONFIRMED`: A fresh `codex app-server` process is started for each HTTP message submission and stopped after turn completion.
- `CONFIRMED`: Message submission is synchronous; browser event streaming is not implemented.
- `CONFIRMED`: `cancel` prevents later gateway submissions but does not yet interrupt a currently executing Codex turn.
- `CONFIRMED`: Unexpected App Server approval requests are declined by the M1 client; an approval UI is not implemented.
- `CONFIRMED`: No HTTP authentication exists; deployment must remain loopback/private-network protected.
- `CONFIRMED`: No browser/frontend exists yet.
- `CONFIRMED`: No Git status/diff implementation exists yet.
- `CONFIRMED`: No PostgreSQL persistence exists yet.
- `CONFIRMED`: No local GPU provider exists yet; the current Codex integration does not use host GPUs.

## Private infrastructure status

Infrastructure identifiers are intentionally not copied into this public repository.

- `PLANNED`: Primary validation target is the selected Red Hat-family IDC Docker host documented in the private device inventory.
- `PLANNED`: Secondary validation target is the selected Ubuntu RTX2080 host documented in the private device inventory.
- `BLOCKED`: Actual remote deployment from this ChatGPT execution environment is blocked because no SSH execution channel/private host credentials are available to the runtime.

Do not describe either target as deployed or validated until host command output proves the acceptance criteria in `docs/DEPLOYMENT.md`.

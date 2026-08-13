# Codex Remote Gateway

Self-hosted web control plane and multi-provider AI gateway for running and supervising Codex-oriented development workflows on Linux.

## Goal

The project provides a browser-oriented control plane for development agents running on Linux. Codex is the first coding-agent integration. Provider selection and future local inference remain separate concerns so Gemini, Groq, local models, and other providers can be added without coupling the control plane to one vendor.

## Current implementation

M1 now contains a server-testable backend slice:

```text
REST API
  -> AgentSessionPort
  -> CodexAgentSessionAdapter
  -> Codex App Server (JSONL/stdin/stdout)
  -> ChatGPT-authenticated Codex runtime
  -> mounted workspace
```

Implemented:

- Spring Boot 4.1 / Java 21 backend.
- Session create/list/get, prompt submission, and cancel APIs.
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
- approval UI;
- persistent gateway session-to-Codex-thread mapping;
- Git status/diff UI;
- local GPU provider;
- multi-agent workflow orchestration.

Because HTTP authentication is not implemented yet, the test deployment binds to `127.0.0.1` by default. Do not expose it directly to the public Internet.

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

The smoke test verifies health, session creation, a real Codex turn, an assistant response, and a second turn that resumes the same Codex thread.

For deployment details, SELinux notes, rollback, acceptance criteria, and the generic SSH deployment helper, see `docs/DEPLOYMENT.md`.

## Architecture direction

```text
Browser
  |
  v
Web Control Plane
  |-- session management
  |-- streaming output/events
  |-- approvals
  |-- Git status/diff
  |
  +--------------------+
  |                    |
  v                    v
Codex App Server    Spring Boot AI Gateway
  |                    |
  |                    +-- OpenAI/Codex-compatible
  |                    +-- Gemini
  |                    +-- Groq
  |                    +-- local provider
  v
Workspace / Runner
```

See `docs/ARCHITECTURE.md` and `docs/ROADMAP.md` for the broader design.

## Design principles

1. Control plane and execution plane remain separate.
2. Provider selection belongs behind explicit adapters/gateways.
3. Risky actions must eventually pass human approval gates.
4. Git remains the primary change ledger.
5. Workspace isolation and least privilege take priority over exposing a convenient shell.
6. Execution state and failures should be observable and reproducible.
7. Delivery proceeds from one workspace/session to multi-agent orchestration incrementally.

## License

Licensed under the Apache License, Version 2.0. See `LICENSE` and `NOTICE`.

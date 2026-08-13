# Current State

Last reviewed: 2026-08-13

## Repository status

- `CONFIRMED`: Public GitHub repository exists at `son1004007/codex-remote-gateway`.
- `CONFIRMED`: License is Apache License 2.0.
- `CONFIRMED`: Product, architecture, security, AI gateway, roadmap, and testing-rule documents exist.
- `IMPLEMENTED`: Repository-level AI working instructions exist in `AGENTS.md`.
- `IMPLEMENTED`: LLM Wiki structure exists under `llm-wiki/`.
- `IMPLEMENTED`: Spring Boot application skeleton exists.
- `IMPLEMENTED`: Maven build uses Spring Boot 4.1.0 and Java 21.
- `IMPLEMENTED`: Basic Actuator health/info exposure is configured.
- `IMPLEMENTED`: A provider-neutral `AgentSessionPort` exists for coding-agent session lifecycle operations.
- `IMPLEMENTED`: An in-memory session adapter exists for development/testing only.
- `IMPLEMENTED`: REST endpoints exist for session create/list/get, message submission, and cancellation.
- `IMPLEMENTED`: Validation and RFC-style problem responses exist for basic request/session errors.
- `IMPLEMENTED`: Initial controller and session-state tests exist.
- `IMPLEMENTED`: GitHub Actions Maven verification workflow exists.

## Current product direction

- `CONFIRMED`: The product is a self-hosted web control plane for AI-assisted development workflows on Linux.
- `CONFIRMED`: Codex is the initial coding-agent target.
- `CONFIRMED`: The system should support web-based session control, event streaming, approval handling, and Git status/diff review.
- `CONFIRMED`: A Spring Boot AI Gateway is planned for provider-neutral access to multiple AI providers.
- `PLANNED`: Initial providers include OpenAI/Codex-compatible access, Gemini, and Groq.
- `PLANNED`: Multi-agent workflow orchestration is a later phase, not MVP scope.

## Current implementation milestone

Roadmap status: `M1 - Single workspace, single Codex session` has started.

Implemented M1 foundation:

```text
Spring Boot API
 -> AgentSessionPort
 -> InMemoryAgentSessionAdapter
 -> session lifecycle/events
```

Next implementation slice:

```text
configured workspace
 -> verified Codex App Server integration
 -> real Codex adapter
 -> streamed agent events
 -> Git diff
```

## Important limitations

- `CONFIRMED`: The current session adapter does not call Codex. It only stores lifecycle state/events in memory.
- `CONFIRMED`: Session state is lost when the application process restarts.
- `CONFIRMED`: No workspace path is currently resolved or authorized from `workspaceId`.
- `CONFIRMED`: No browser/frontend exists yet.
- `CONFIRMED`: No authentication exists yet.
- `CONFIRMED`: No WebSocket/SSE event stream exists yet.
- `CONFIRMED`: No Git inspection implementation exists yet.
- `CONFIRMED`: No PostgreSQL persistence exists yet.

## What does not exist yet

- real Codex integration adapter
- Codex App Server transport/client implementation
- workspace registry/path allowlist
- frontend application
- WebSocket/SSE streaming implementation
- authentication/authorization implementation
- PostgreSQL schema/migrations
- Git status/diff service
- approval workflow
- Docker runner
- Gemini/Groq provider implementations
- workflow engine
- deployment automation

Do not describe any of these as implemented until verified in source code.

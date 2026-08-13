# Current State

Last reviewed: 2026-08-13

## Repository status

- `CONFIRMED`: Public GitHub repository exists at `son1004007/codex-remote-gateway`.
- `CONFIRMED`: License is Apache License 2.0.
- `CONFIRMED`: Product, architecture, security, AI gateway, and roadmap documents exist under `docs/`.
- `IMPLEMENTED`: Repository-level AI working instructions exist in `AGENTS.md`.
- `IMPLEMENTED`: LLM Wiki structure exists under `llm-wiki/`.
- `PLANNED`: Application source code has not yet been implemented.

## Current product direction

- `CONFIRMED`: The product is a self-hosted web control plane for AI-assisted development workflows on Linux.
- `CONFIRMED`: Codex is the initial coding-agent target.
- `CONFIRMED`: The system should support web-based session control, event streaming, approval handling, and Git status/diff review.
- `CONFIRMED`: A Spring Boot AI Gateway is planned for provider-neutral access to multiple AI providers.
- `PLANNED`: Initial providers include OpenAI/Codex-compatible access, Gemini, and Groq.
- `PLANNED`: Multi-agent workflow orchestration is a later phase, not MVP scope.

## Current implementation milestone

Roadmap status is effectively `M0 - Repository and decisions`.

Next vertical slice:

```text
Browser
 -> Spring Boot API
 -> one configured workspace
 -> Codex adapter
 -> one persistent session
 -> streamed events
 -> Git diff
```

## What does not exist yet

- Spring Boot application skeleton
- frontend application
- Codex integration adapter implementation
- Codex App Server integration
- WebSocket/SSE implementation
- authentication implementation
- PostgreSQL schema/migrations
- Docker runner
- Gemini/Groq provider implementations
- workflow engine
- deployment automation

Do not describe any of these as implemented until verified in source code.

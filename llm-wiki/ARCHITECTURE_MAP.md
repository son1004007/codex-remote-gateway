# Architecture Map

This is a compact navigation map. Detailed design belongs in `docs/ARCHITECTURE.md`.

## Planned component boundaries

```text
Browser / Mobile Web
        |
        v
Spring Boot Control Plane
  |-- Authentication / Authorization
  |-- Workspace Registry
  |-- Session Management
  |-- Event Streaming
  |-- Approval Workflow
  |-- Git Inspection
  |-- Audit / Persistence
  |
  +--> Codex Adapter
  |       |
  |       v
  |    Codex Runtime / App Server
  |       |
  |       v
  |    Workspace Runner
  |
  +--> AI Gateway
          |-- OpenAI/Codex-compatible provider
          |-- Gemini provider
          |-- Groq provider
          `-- future providers

PostgreSQL
  `-- durable control-plane state
```

## Responsibility rules

### Web UI

Owns presentation and user interaction only. It must not hold provider credentials or directly execute host commands.

### Control Plane

Owns authorization, session/workspace lifecycle, approvals, persistence, audit, and safe orchestration.

### Codex Adapter

Owns translation between the control plane's stable agent-session contract and the currently supported Codex runtime protocol.

### AI Gateway

Owns generic AI-provider abstraction, provider/model selection, normalized errors, telemetry, and provider credentials.

### Runner

Owns controlled execution of project commands/tools inside an explicitly allowed workspace and security boundary.

### PostgreSQL

Owns durable state. It is not intended to be the high-volume terminal-log store indefinitely if raw event volume becomes large.

## Future component

### Workflow Engine

Post-MVP. Owns task dependencies, isolated parallel tasks, handoff artifacts, retries/resume, and human approval nodes.

It must not be introduced by allowing multiple agents to mutate a shared working tree without isolation.

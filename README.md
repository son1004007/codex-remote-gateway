# Codex Remote Gateway

Self-hosted web control plane and multi-provider AI gateway for running and supervising Codex-oriented development workflows on Linux.

## Goal

This project aims to provide a browser-accessible control plane for development agents running on a Linux server. The initial target is Codex, while the architecture keeps AI-provider selection separate so Gemini, Groq, OpenAI/Codex-compatible providers, and future providers can be added without coupling the web UI to a single model vendor.

The project is intended to make remote AI-assisted development observable, controllable, and reproducible rather than merely exposing a shell over the web.

## Core requirements

- Run Codex-oriented development workflows on a Linux host.
- Control sessions from a web browser.
- Stream agent output and execution events in real time.
- Inspect approval requests before privileged or destructive operations.
- Review Git status and diffs produced by an agent.
- Separate the web/control plane from model-provider integrations.
- Support provider selection through a Spring Boot AI gateway.
- Isolate workspaces and execution environments with containers where appropriate.
- Preserve session, execution, approval, and audit history.
- Allow later expansion into a multi-agent control plane.

## Proposed architecture

```text
Browser
  |
  v
Web Control Plane
  |-- session management
  |-- streaming output/events
  |-- approvals
  |-- Git status/diff
  |-- execution/audit history
  |
  +--------------------+
  |                    |
  v                    v
Codex App Server    Spring Boot AI Gateway
  |                    |
  |                    +-- OpenAI / Codex-compatible provider
  |                    +-- Gemini
  |                    +-- Groq
  |                    +-- future providers
  v
Workspace / Runner
  |
  +-- Git repository
  +-- build/test tools
  +-- Docker-isolated execution
```

See `docs/ARCHITECTURE.md` for the detailed design.

## Design principles

1. **Control plane and execution plane are separate.** The browser should not directly expose a privileged host shell.
2. **Provider-neutral orchestration.** Model/provider selection belongs behind an explicit gateway interface.
3. **Human approval for risky actions.** Destructive commands, credential access, deployment, and other privileged operations should support approval gates.
4. **Git is the primary change ledger.** Agent-produced source changes should remain reviewable through status, diff, commits, and pull requests.
5. **Isolation by default.** Untrusted or project-specific execution should be isolated from the host whenever practical.
6. **Observable execution.** Sessions, commands, tool events, approvals, failures, and outputs should be traceable.
7. **Incremental delivery.** Start with one Codex session and one workspace before introducing multi-agent orchestration.

## Initial scope

### Phase 1 - Remote Codex MVP

- Linux-hosted service
- web login
- workspace registration
- start/stop Codex sessions
- streaming logs/events
- prompt submission
- approval request UI
- Git status/diff view
- basic session history

### Phase 2 - AI Gateway

- Spring Boot provider abstraction
- provider/model configuration
- OpenAI/Codex-compatible integration
- Gemini integration
- Groq integration
- timeout/retry/rate-limit policy
- usage and error telemetry

### Phase 3 - Safe execution

- containerized runner
- per-workspace filesystem boundaries
- command policy
- secrets isolation
- resource limits
- audit trail

### Phase 4 - Agent Control Plane

- multiple agent roles
- dependency-aware task execution
- sequential and parallel workflow steps
- shared artifacts and handoff context
- workflow state visualization
- failure/retry/resume controls

## Repository status

This repository currently contains the product and architecture plan. Implementation should follow the milestones in `docs/ROADMAP.md` rather than attempting the complete control plane in one step.

## License

Licensed under the Apache License, Version 2.0. You may use, modify, distribute, and use this project commercially subject to the terms of the license. See `LICENSE` and `NOTICE`.

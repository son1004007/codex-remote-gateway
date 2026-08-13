# Work Log

Record only meaningful milestones, reversals, migrations, or incidents. Current truth belongs in `CURRENT_STATE.md`, not here.

## 2026-08-13 — Repository planning initialized

- Public repository created.
- Apache License 2.0 selected.
- Added product, architecture, security, AI gateway, and roadmap documentation.
- Defined the project as a self-hosted Linux web control plane for Codex-oriented development workflows with a future multi-provider AI Gateway and multi-agent control plane.

## 2026-08-13 — LLM Wiki initialized

- Added root `AGENTS.md` as the AI-agent entry point.
- Added explicit status vocabulary: confirmed, implemented, planned, proposed, blocked, unknown, deprecated.
- Separated current implementation state from architectural decisions and unresolved questions.
- Added constraints preventing agents from inventing provider behavior, credentials, ports, deployment state, or completed work.
- Added known-issues register and compact architecture map.

Reason:

Long-lived AI-assisted repositories are vulnerable to context drift. The wiki provides durable repository-local context so future Codex/ChatGPT/other agent sessions can reconstruct decisions before modifying code.

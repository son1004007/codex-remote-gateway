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

## 2026-08-13 — ISTQB-based default test rules added

- Added `llm-wiki/TESTING_RULES.md`.
- A generic request such as `test this` now maps to the repository's ISTQB-oriented test process.
- Test work should be separated from implementation work as an independent Test Agent role when the execution environment supports separate agents.
- Boundary value, equivalence partitioning, decision table, state transition, negative, regression, and risk-based testing are part of the default approach.

## 2026-08-13 — M1 implementation started

- Added Spring Boot 4.1.0 / Java 21 Maven project skeleton.
- Added application entry point and Actuator health configuration.
- Added `AgentSessionPort` as the initial agent-session boundary.
- Added an in-memory adapter for development/testing; this is not a Codex integration.
- Added REST APIs for session create/list/get, message submission, and cancellation.
- Added validation/problem-detail error handling.
- Added controller contract tests and session state-transition unit tests.
- Added GitHub Actions Maven verification.

Reason:

The first implementation slice intentionally validates the control-plane domain/API boundary before coupling it to the current Codex App Server protocol. The real Codex adapter remains a separate next step and must be verified against the official interface before implementation.

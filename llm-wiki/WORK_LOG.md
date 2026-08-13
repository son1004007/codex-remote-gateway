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
- A generic request such as `test this` maps to the repository's ISTQB-oriented test process.
- Test work should be separated from implementation work as an independent Test Agent role when the execution environment supports separate agents.
- Boundary value, equivalence partitioning, decision table, state transition, negative, regression, and risk-based testing are part of the default approach.

## 2026-08-13 — M1 implementation started

- Added Spring Boot 4.1.0 / Java 21 Maven project skeleton.
- Added application entry point and Actuator health configuration.
- Added `AgentSessionPort` as the initial agent-session boundary.
- Added an in-memory adapter for development/testing.
- Added REST APIs for session create/list/get, message submission, and cancellation.
- Added validation/problem-detail error handling.
- Added controller contract tests and session state-transition unit tests.
- Added GitHub Actions Maven verification.

Reason:

The first slice established the control-plane domain/API boundary before coupling it to the Codex App Server protocol.

## 2026-08-13 — Codex App Server and container deployment slice implemented

- Verified the current App Server lifecycle before implementation: initialize, initialized, thread start/resume, turn start, event notifications, turn completed.
- Added `CodexAppServerClient` using JSONL stdio and a `CodexAgentSessionAdapter` selectable through configuration.
- Added provider thread binding so sequential HTTP requests can resume one Codex thread while the gateway process remains alive.
- Added configured workspace-root enforcement.
- Added UBI 9 / Node.js 22 / Java 21 container runtime with Codex CLI pinned to `0.147.0`.
- Added Compose configuration, persistent Codex home, mounted workspaces, device-auth helper, deployment preflight, health polling, remote SSH helper, and two-turn smoke test.
- Kept the gateway bound to loopback because application authentication is not implemented yet.
- Added CI checks for Java tests, shell syntax, Compose model, container image build, and image-local Codex version.

Testing findings during this slice:

1. `CONFIRMED` workspace symlink escape: lexical `startsWith` validation allowed a workspace-local symlink to resolve outside the allowed root. A negative regression test reproduced it. Fixed by comparing real paths.
2. `CONFIRMED` UBI package conflict: installing full `curl` conflicted with the base image's `curl-minimal`. Removed the redundant package installation.
3. `CONFIRMED` generated-script execution issue: scripts created through repository APIs do not necessarily retain executable mode. Internal helper calls were changed to explicit `bash` invocation.
4. `CONFIRMED` remote path expansion issue: a quoted `~` default prevented remote shell tilde expansion. The default remote directory is now home-relative without a quoted tilde.
5. `PROPOSED/mitigated`: bind-mounted Codex state can be owned by the host user rather than container UID 1001. Added a Docker-root volume preparation helper that fixes ownership without recursively changing arbitrary project workspaces.

Private infrastructure planning:

- A separate private device-inventory deployment record selects the IDC Red Hat-family Docker host as primary compatibility validation and the Ubuntu RTX2080 server as secondary validation/future local-model host.
- Infrastructure identifiers and credential references remain outside this public repository.
- Actual host deployment is still blocked from this ChatGPT execution environment because no SSH execution channel/private host credential is exposed to the runtime.

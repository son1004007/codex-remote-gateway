# Current State

Last reviewed: 2026-08-17

## Repository status

- `CONFIRMED`: Public GitHub repository exists at `son1004007/codex-remote-gateway`.
- `CONFIRMED`: License is Apache License 2.0.
- `IMPLEMENTED`: Repository-level AI instructions and LLM Wiki exist.
- `IMPLEMENTED`: Spring Boot 4.1.0 / Java 21 backend exists.
- `IMPLEMENTED`: Basic Actuator health/info exposure exists.
- `IMPLEMENTED`: Provider-neutral `AgentSessionPort` exists.
- `IMPLEMENTED`: In-memory and Codex-backed session adapters exist; Codex communicates with `codex app-server` over JSONL stdio.
- `IMPLEMENTED`: Codex initialize, thread start/resume, turn start, streamed agent-message collection, and turn-completed handling exist.
- `IMPLEMENTED`: Workspace resolution accepts only a direct configured-root child and rejects traversal, nested paths, absolute-path escape, invalid input, and symlink escape.
- `IMPLEMENTED`: One active gateway session and one active workflow per workspace are enforced.
- `IMPLEMENTED`: REST endpoints exist for session lifecycle and workflow `create`, `list/status`, `approve`, `resume`, and `cancel`.
- `IMPLEMENTED`: Workflow execution is asynchronous on Java 21 virtual threads.
- `IMPLEMENTED`: Deployment is a human approval gate by default; `autoDeploy=true` must be explicit.
- `IMPLEMENTED`: Workflow worker output requires a structured `WORKFLOW_RESULT` marker; missing markers fail closed as `BLOCKED`.
- `IMPLEMENTED`: UBI 9 + Node.js 22 + Java 21 + pinned Codex CLI container definition and deployment automation exist.

## Cross-model workflow

PR: `#2 Add verified Codex and Gemini workflow routing`

The verified sequential workflow is:

```text
PLAN             -> Antigravity/Gemini preferred
PLAN_VERIFY      -> Codex
IMPLEMENT        -> Codex
TEST_DESIGN      -> Antigravity/Gemini preferred
TEST             -> Codex + executable tools
REVIEW           -> Antigravity/Gemini preferred
REVIEW_VERIFY    -> Codex + reproduction/tests
DEPLOY           -> Codex for now, after human approval
E2E              -> Codex for now
```

- `IMPLEMENTED ON PR #2`: `RoutingWorkflowWorkerAdapter` selects a worker by stage.
- `IMPLEMENTED ON PR #2`: when `GATEWAY_ANTIGRAVITY_ENABLED=false`, all stages safely fall back to Codex.
- `IMPLEMENTED ON PR #2`: each stage stores bounded `stageOutputs` and `stageWorkers`, so later providers receive explicit handoff evidence instead of relying on hidden conversation memory.
- `IMPLEMENTED ON PR #2`: plans remain provisional until `PLAN_VERIFY` independently checks them against the real repository.
- `IMPLEMENTED ON PR #2`: Gemini review findings remain provisional until `REVIEW_VERIFY` reproduces, disproves, or fixes them using Codex and executable evidence.
- `IMPLEMENTED ON PR #2`: Antigravity analysis runs against a disposable workspace snapshot, excluding symlinks and common build/cache directories, with bounded read-only Git status/diff context. It does not receive the mutable real checkout.
- `IMPLEMENTED ON PR #2`: Antigravity uses `agy --prompt ... --sandbox` only when explicitly enabled. Model selection is configurable and blank by default so a Flash model is not falsely assumed before host verification.
- `CONFIRMED`: PR #2 CI run 64 passed Java compilation/tests, shell syntax, deployment-script tests, Compose validation, UBI image build, and image-local Codex CLI verification.
- `PENDING RUNTIME`: an actual Linux host must have Antigravity CLI installed/authenticated and execute a real collaborative workflow before the Gemini integration is described as runtime-validated.

## Product direction

- `CONFIRMED`: The product is a self-hosted control plane for AI-assisted development workflows on Linux.
- `CONFIRMED`: The desired mobile human control surface is intentionally small: start work, inspect status, approve guarded actions, resume blocked work, and cancel.
- `CONFIRMED`: AI-generated plans and reviews are claims, not evidence.
- `CONFIRMED`: Codex is the primary mutable-workspace implementation and execution agent.
- `CONFIRMED`: Gemini/Antigravity is assigned independent broad-context planning, adversarial test design, and review; its findings are verified before mutation or progression.
- `PLANNED`: deterministic deployment/E2E runner should replace AI-owned deployment mechanics.
- `PLANNED`: PostgreSQL persistence and GitHub Issue/PR/check integration should become the durable workflow ledger.
- `PLANNED`: bounded parallel work should wait until sequential cross-model orchestration, persistence, and isolated worktrees are stable.

## Verification evidence

- `CONFIRMED`: Java regression tests cover session lifecycle, API behavior, Codex App Server fake-protocol start/resume/error handling, workspace isolation, workflow deployment gates, auto-deploy behavior, and one-active-workflow-per-workspace protection.
- `CONFIRMED`: routing tests assert `PLAN`, `TEST_DESIGN`, and `REVIEW` route to Antigravity when enabled, while verification, implementation, execution, deploy, and E2E route to Codex.
- `CONFIRMED`: fallback tests assert every stage routes to Codex when Antigravity is disabled.
- `CONFIRMED`: handoff tests assert Gemini plan/review evidence appears in later Codex verification prompts and per-stage provider evidence is retained.
- `CONFIRMED`: shell/deployment-script tests and Compose validation pass with the new optional Antigravity configuration.
- `CONFIRMED`: UBI Codex image build and pinned Codex CLI validation pass.
- `UNKNOWN`: real Antigravity CLI non-interactive behavior on the selected private servers until host execution proves it.

## Important limitations

- Gateway session and workflow state remain in memory and are lost on Spring Boot restart.
- Codex thread files may persist under `CODEX_HOME`, but gateway workflow/session mappings are not persisted.
- Antigravity installation and authentication are not currently bundled into the UBI container.
- Antigravity analysis is stateless per stage; continuity is supplied by explicit workflow handoff evidence.
- Antigravity receives a disposable source snapshot rather than the real `.git` directory; bounded Git status/diff text is supplied separately.
- Existing Codex cancel semantics do not yet guarantee interruption of an already executing Codex turn.
- HTTP authentication, browser UI, SSE/WebSocket streaming, Git status/diff UI, and PostgreSQL persistence are not implemented.
- `DEPLOY` and `E2E` still route to Codex and should be moved to a controlled deterministic runner.
- No local GPU provider exists; the current Codex integration does not use host GPUs.

## Private infrastructure status

Infrastructure identifiers are intentionally not copied into this public repository.

- `PLANNED`: Primary validation target is the selected Red Hat-family IDC Docker host documented in the private device inventory.
- `PLANNED`: Secondary validation target is the selected Ubuntu RTX2080 host documented in the private device inventory.
- `UNKNOWN`: Collaborative Antigravity runtime availability/authentication on either host.

Do not describe either target or the Codex+Antigravity workflow as deployed/validated until actual host output proves the acceptance criteria.

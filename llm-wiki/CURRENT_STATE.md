# Current State

Last reviewed: 2026-08-17

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
- `IMPLEMENTED`: Workspace resolution accepts only a direct configured-root child and rejects parent traversal, root aliases, nested paths, absolute-path escape, invalid path input, and symlink escape.
- `IMPLEMENTED`: M1 permits one active gateway session per workspace; a second create request returns a conflict until the first session is cancelled.
- `IMPLEMENTED`: REST endpoints exist for session create/list/get, message submission, and cancellation.
- `IMPLEMENTED`: Validation and problem-detail responses exist for request/session/workspace/Codex errors.
- `IMPLEMENTED`: A first workflow-controller slice exists above the session layer with `create`, `list/status`, `approve`, `resume`, and `cancel` APIs.
- `IMPLEMENTED`: Workflow execution is asynchronous on Java 21 virtual threads and follows `PLAN -> IMPLEMENT -> TEST -> REVIEW -> DEPLOY -> E2E`.
- `IMPLEMENTED`: Deployment is a human approval gate by default; `autoDeploy=true` must be explicitly requested to bypass it.
- `IMPLEMENTED`: Codex workflow responses must end with a structured `WORKFLOW_RESULT` marker; missing markers fail closed as `BLOCKED` instead of being assumed successful.
- `IMPLEMENTED`: `WorkflowWorkerPort` separates orchestration from provider execution, with in-memory and Codex worker adapters as the first implementations.
- `IMPLEMENTED`: One active workflow per workspace is enforced to avoid uncontrolled concurrent mutation of the same checkout.
- `IMPLEMENTED`: UBI 9 + Node.js 22 + Java 21 + pinned Codex CLI container definition exists.
- `IMPLEMENTED`: Docker Compose, preflight, runtime-volume preparation, device-login, deploy, remote-target, and two-turn smoke-test automation exists. Deployment scripts consume the same safe `.env` settings as Compose.
- `IMPLEMENTED`: Remote synchronization is clone-or-clean-fast-forward only and refuses a dirty, diverged, non-main, or origin-mismatched remote checkout.
- `IMPLEMENTED`: GitHub Actions verifies Java tests, shell syntax, Compose configuration, UBI image build, and image-local Codex CLI version.

## Current product direction

- `CONFIRMED`: The product is a self-hosted control plane for AI-assisted development workflows on Linux.
- `CONFIRMED`: The desired human control surface is intentionally small: start work, inspect status, approve guarded actions, resume blocked work, and cancel.
- `CONFIRMED`: Codex is the initial coding-agent target.
- `CONFIRMED`: The system should support web/mobile-oriented session and workflow control, event/status inspection, approval handling, and Git status/diff review.
- `CONFIRMED`: A Spring Boot AI Gateway is planned for provider-neutral access to multiple AI providers.
- `PLANNED`: Initial additional providers include Gemini/Antigravity, Groq, and a separately isolated local-model provider.
- `IMPLEMENTED`: Sequential workflow orchestration has started before full multi-agent DAG support because it directly reduces human management points without requiring parallel mutable workspaces.
- `PLANNED`: Stage-aware routing should eventually use Codex primarily for implementation/testing and an independent provider such as Gemini/Antigravity for planning/review where useful.

## Current implementation milestone

Roadmap status: `M1 - Single workspace, single Codex session` remains server-testable but not yet validated on the selected private infrastructure. A bounded workflow-controller slice has now been added above M1; it does not claim that the later full multi-agent workflow engine is complete.

Current session path:

```text
Spring Boot API
 -> AgentSessionPort
 -> CodexAgentSessionAdapter
 -> Codex App Server process per submitted turn
 -> ChatGPT-authenticated Codex state under persistent CODEX_HOME
 -> mounted workspace
```

Current workflow path:

```text
Client / future mobile ChatGPT control surface
 -> /api/v1/workflows
 -> WorkflowService
 -> WorkflowWorkerPort
 -> CodexWorkflowWorkerAdapter
 -> AgentSessionPort
 -> PLAN -> IMPLEMENT -> TEST -> REVIEW
 -> human DEPLOY approval by default
 -> DEPLOY -> E2E
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

- `CONFIRMED`: Java regression tests include session lifecycle, API behavior, Codex App Server fake-protocol start/resume/error handling, workspace-isolation, and one-active-session-per-workspace cases.
- `ADDED`: Workflow-service regression tests cover the default deployment approval gate, explicit auto-deploy behavior, completion after approval, and one-active-workflow-per-workspace protection.
- `CONFIRMED`: Independent protocol probing against the pinned Codex CLI version established that the sandbox token is `workspace-write`; unsupported `thread/resume.excludeTurns` is omitted unless the corresponding experimental capability is negotiated.
- `CONFIRMED`: Deployment-script regression tests cover safe dotenv loading, non-destructive remote synchronization, unsafe remote input rejection, and status-first Codex login behavior.
- `CONFIRMED`: An ISTQB-oriented negative test reproduced a symlink workspace escape before the implementation was fixed.
- `CONFIRMED`: The symlink-escape regression test passes after real-path enforcement.
- `CONFIRMED`: An initial UBI container build exposed a `curl-minimal` versus `curl` package conflict; the redundant `curl` install was removed.
- `UNKNOWN`: The new workflow-controller branch still requires GitHub Actions CI results before it can be described as merged/validated.
- `UNKNOWN`: Real ChatGPT-authenticated Codex workflow behavior on the selected private servers remains unverified until remote execution proves the acceptance criteria.

## Important limitations

- `CONFIRMED`: Gateway session state remains in memory and is lost when Spring Boot restarts.
- `CONFIRMED`: Workflow/task state and events are also currently in memory and are lost on restart.
- `CONFIRMED`: Codex thread files can persist in `CODEX_HOME`, but the gateway session-to-thread and workflow-to-session mappings are not persisted.
- `CONFIRMED`: A fresh `codex app-server` process is started for each HTTP message submission and stopped after turn completion.
- `CONFIRMED`: Session message submission is synchronous; the workflow controller moves this blocking work onto virtual threads so workflow API creation does not wait for all stages to finish.
- `CONFIRMED`: Existing `cancel` prevents later gateway submissions but does not yet interrupt a currently executing Codex turn.
- `CONFIRMED`: Unexpected App Server approval requests are declined by the M1 client; workflow deployment approval is a controller-level gate, not an App Server approval UI.
- `CONFIRMED`: No HTTP authentication exists; deployment must remain loopback/private-network protected.
- `CONFIRMED`: No browser/frontend exists yet.
- `CONFIRMED`: No Git status/diff implementation exists yet.
- `CONFIRMED`: No PostgreSQL persistence exists yet.
- `CONFIRMED`: No Gemini/Antigravity workflow worker exists yet.
- `CONFIRMED`: With `gateway.agent.mode=codex`, planning and review are role-separated prompts but still use the same Codex provider/session, so review is not yet provider-independent.
- `CONFIRMED`: No local GPU provider exists yet; the current Codex integration does not use host GPUs.

## Private infrastructure status

Infrastructure identifiers are intentionally not copied into this public repository.

- `PLANNED`: Primary validation target is the selected Red Hat-family IDC Docker host documented in the private device inventory.
- `PLANNED`: Secondary validation target is the selected Ubuntu RTX2080 host documented in the private device inventory.
- `UNKNOWN`: Actual remote deployment and ChatGPT-authenticated turns remain unverified until private-host command output proves the acceptance criteria. Infrastructure identifiers and credentials remain outside this repository.

Do not describe either target as deployed or validated until host command output proves the acceptance criteria in `docs/DEPLOYMENT.md`.

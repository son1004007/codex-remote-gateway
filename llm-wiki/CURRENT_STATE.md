# Current State

Last reviewed: 2026-08-19

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

## Browser Codex control

PR: `#3 Add first browser Codex control vertical slice`

- `IMPLEMENTED ON PR #3`: responsive PC/mobile browser UI is served from the gateway root.
- `IMPLEMENTED ON PR #3`: direct-child workspace discovery is available in Codex mode without accepting arbitrary filesystem paths.
- `IMPLEMENTED ON PR #3`: session create/list/select/reconnect and prompt submission are available from the browser.
- `IMPLEMENTED ON PR #3`: browser prompt requests enqueue a Codex turn on a Java 21 virtual thread and return `202`, so the HTTP request/browser connection does not own the entire turn lifetime.
- `IMPLEMENTED ON PR #3`: execution state can be polled as `IDLE`, `RUNNING`, `CANCEL_REQUESTED`, `SUCCEEDED`, `FAILED`, or `CANCELLED`.
- `IMPLEMENTED ON PR #3`: cancellation is best effort and closes the pre-start cancellation race, but the underlying Codex App Server client still cannot guarantee interruption after every provider operation has begun.
- `IMPLEMENTED ON PR #3`: session events are rendered as a browser timeline; model/user text is inserted with DOM `textContent`, not trusted HTML.
- `IMPLEMENTED ON PR #3`: read-only Git status, working-tree diff, and staged diff APIs/UI use fixed ProcessBuilder arguments, no browser shell, a timeout, and bounded returned output.
- `IMPLEMENTED ON PR #3`: Git worktree `.git` files are recognized as repositories.
- `IMPLEMENTED ON PR #3`: deployment smoke test now checks the browser root and asynchronous two-turn Codex thread resume behavior.
- `SECURITY BOUNDARY`: the browser control surface is not authenticated yet and must remain loopback/private-network bound until authenticated HTTPS ingress is implemented.
- `PENDING RUNTIME`: PR #3 must pass CI, then be deployed and smoke-tested independently on the primary IDC target and the secondary RTX2080 target before server-local browser control is called runtime-validated.

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
- `IMPLEMENTED ON PR #2`: Antigravity analysis runs against a disposable workspace snapshot and does not receive the mutable real checkout.
- `CONFIRMED`: PR #2 CI run 64 passed Java compilation/tests, shell syntax, deployment-script tests, Compose validation, UBI image build, and image-local Codex CLI verification.
- `PENDING RUNTIME`: actual Linux-host Antigravity CLI installation/authentication and collaborative execution remain unverified.

## Product direction

- `CONFIRMED`: The primary product is a self-hosted **per-server Codex Web GUI**. Each Linux server owns its local gateway, Codex runtime, and workspaces; a central controller is not required.
- `CONFIRMED`: The first runtime targets are the existing primary IDC Linux server and existing RTX2080 Linux server documented in the private device inventory.
- `CONFIRMED`: GitHub is the durable source/change/evidence handoff between ChatGPT and Codex, while the Web UI is the direct interactive control surface for the Codex instance on that server.
- `CONFIRMED`: The desired mobile human control surface is intentionally small: choose workspace/session, send work, inspect progress/results, stop work, and inspect Git changes.
- `CONFIRMED`: AI-generated plans and reviews are claims, not evidence.
- `CONFIRMED`: Codex is the primary mutable-workspace implementation and execution agent.
- `CONFIRMED`: Multi-agent workflow/controller features are optional higher-level capabilities and must not make the basic per-server GUI dependent on a central service.
- `PLANNED`: authenticated HTTPS browser ingress is the next network-facing slice after local browser-control validation.
- `PLANNED`: true incremental SSE/WebSocket event streaming should replace 1-second UI polling when the underlying Codex events are exposed safely.
- `PLANNED`: persisted session metadata/thread mapping should permit gateway restart and later reconnect.

## Verification evidence

- `CONFIRMED`: Java regression tests cover session lifecycle, API behavior, Codex App Server fake-protocol start/resume/error handling, workspace isolation, workflow deployment gates, auto-deploy behavior, and one-active-workflow-per-workspace protection.
- `CONFIRMED`: routing/fallback/handoff tests cover the existing Codex + Antigravity workflow policy.
- `CONFIRMED`: shell/deployment-script tests and Compose validation exist.
- `CONFIRMED`: UBI Codex image build and pinned Codex CLI validation exist in CI.
- `PENDING`: PR #3 final CI evidence after the current browser-control fixes.
- `PENDING`: browser-control deployment and real Codex two-turn smoke evidence on both selected Linux servers.

## Important limitations

- Gateway session and workflow state remain in memory and are lost on Spring Boot restart.
- Codex thread files may persist under `CODEX_HOME`, but gateway workflow/session mappings are not persisted.
- Browser updates currently use polling rather than SSE/WebSocket incremental streaming.
- The session timeline currently receives the bounded/final Codex messages exposed by the existing adapter; it is not yet a full terminal-like command/event stream.
- Existing Codex cancel semantics do not guarantee interruption of every already executing Codex turn.
- HTTP authentication/authorization is not implemented. Keep the service loopback/private until authenticated HTTPS ingress is verified.
- Git browser controls are read-only in the first slice; commit/push/destructive actions are intentionally not exposed yet.
- Antigravity installation/authentication is not bundled into the current UBI image.
- `DEPLOY` and `E2E` workflow stages still route to Codex and should later move to a controlled deterministic runner.
- No local GPU provider exists; the current Codex integration does not use host GPUs.

## Private infrastructure status

Infrastructure identifiers are intentionally not copied into this public repository.

- `PLANNED`: Primary runtime validation is the selected Red Hat-family IDC target documented in private device inventory.
- `PLANNED`: Secondary runtime validation is the selected Ubuntu RTX2080 target documented in private device inventory.
- `UNKNOWN`: PR #3 runtime status on each target until deployment workflow output proves the acceptance criteria.

Do not describe either target as browser-GUI validated until actual host deployment and smoke output proves it.

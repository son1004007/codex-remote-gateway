# Current State

Last reviewed: 2026-08-20

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

PR: `#3 Add first browser Codex control vertical slice` — merged.

- `IMPLEMENTED`: responsive PC/mobile browser UI is served from the gateway root.
- `IMPLEMENTED`: direct-child workspace discovery is available in Codex mode without accepting arbitrary filesystem paths.
- `IMPLEMENTED`: session create/list/select/reconnect and prompt submission are available from the browser.
- `IMPLEMENTED`: browser prompt requests enqueue a Codex turn on a Java 21 virtual thread and return `202`, so the HTTP request/browser connection does not own the entire turn lifetime.
- `IMPLEMENTED`: execution state can be polled as `IDLE`, `RUNNING`, `CANCEL_REQUESTED`, `SUCCEEDED`, `FAILED`, or `CANCELLED`.
- `IMPLEMENTED`: cancellation is best effort and closes the pre-start cancellation race, but the underlying Codex App Server client still cannot guarantee interruption after every provider operation has begun.
- `IMPLEMENTED`: session events are rendered as a browser timeline; model/user text is inserted with DOM `textContent`, not trusted HTML.
- `IMPLEMENTED`: read-only Git status, working-tree diff, and staged diff APIs/UI use fixed ProcessBuilder arguments, no browser shell, a timeout, and bounded returned output.
- `IMPLEMENTED`: Git worktree `.git` files are recognized as repositories.
- `IMPLEMENTED`: deployment smoke test checks the browser root and asynchronous two-turn Codex thread resume behavior.
- `CONFIRMED`: PR #3 final CI run `32251726719` passed Java tests, shell/deployment checks, Compose validation, UBI image build, and image-local Codex CLI verification.
- `CONFIRMED`: merge SHA is `46daa5b1b8891eb7916e0c3075fb7255e0d79589`.
- `CONFIRMED`: rollout trigger commit is `91fc451861bfc87d819c33a707e7894f7762333d` and `.deploy/trigger` was changed once for this release.
- `SECURITY BOUNDARY`: the browser control surface is not authenticated yet and must remain loopback/private-network bound until authenticated HTTPS ingress is implemented.
- `PENDING RUNTIME`: browser control still requires live validation on both `office` and `idc` hosts.

## Server target naming and connection metadata

Use only these target identities in application code, workflows, docs, and status output:

```text
office = office RTX2080 server
idc    = IDC data1 server
```

Do not use `primary` or `secondary` as server identities. They caused role ambiguity and are not stable properties of a host.

- `IMPLEMENTED`: non-secret host/port/user/runtime metadata is versioned in `config/server-targets.properties`.
- `IMPLEMENTED`: `scripts/load-server-target.sh` accepts only `office` or `idc`, validates the checked-in values, and exports the connection fields without shell-evaluating the file.
- `IMPLEMENTED`: CI tests reject the legacy target names `primary` and `secondary`.
- `CONFIRMED`: `office` is the RTX2080 host and uses the host-local runtime path.
- `CONFIRMED`: `idc` is the IDC `data1` host and uses the Compose runtime path.
- `SECURITY BOUNDARY`: SSH private keys and pinned `known_hosts` material remain GitHub Environment secrets. Host, port, and user are not treated as secrets.
- `TEMPORARY INTERNAL DETAIL`: existing GitHub Environment secret scopes still have legacy names `server-primary` and `server-secondary`; workflows map `office` to the former and `idc` to the latter only to preserve existing SSH authentication material. User-facing/job/status terminology is `office`/`idc`.

## Runtime verification observability

- `IMPLEMENTED`: fixed Issue #4 is the durable runtime verification pointer.
- `IMPLEMENTED`: reopening Issue #4 by the repository owner runs a read-only `office`/`idc` verifier without changing `.deploy/trigger` or deployment state.
- `IMPLEMENTED`: verifier checks expected deployed SHA, health, browser UI/workspace API, and prior real-Codex two-turn/thread evidence.
- `IMPLEMENTED`: SSH/runtime failures are reduced to safe categories rather than printing authentication material.
- `CONFIRMED`: previous run `32253686227` failed because host/port/user were incorrectly expected as Environment secrets. That configuration model is being replaced by checked-in non-secret target metadata.
- `NEXT`: merge the checked-in target configuration, rerun Issue #4, and diagnose the next real host/runtime condition from the resulting evidence.

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
- `CONFIRMED`: The first runtime targets are `office` (RTX2080) and `idc` (data1).
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
- `CONFIRMED`: browser-control PR #3 final CI is PASS.
- `CONFIRMED`: runtime verifier diagnostic PRs #5, #6, and #7 are merged with CI coverage.
- `PENDING`: live `office` and `idc` browser-control/runtime verification after the target-metadata change is merged.

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

## Runtime target status

- `office`: RTX2080 host, target metadata checked in, SSH authentication material remains in its legacy Environment secret scope, live browser-GUI verification pending.
- `idc`: data1 host, target metadata checked in, SSH authentication material remains in its legacy Environment secret scope, live browser-GUI verification pending.

Do not describe either target as browser-GUI validated until actual host deployment and smoke output proves it.

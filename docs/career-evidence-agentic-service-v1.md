# Career-Evidence Agentic Service V1

Status: `PROPOSED`

As of: 2026-09-08

## Purpose

Turn the already implemented Codex Remote Gateway into a stronger, directly demonstrable **AI application / agent runtime integration** portfolio artifact.

This is not a replacement product direction. It extends the existing self-hosted per-server Codex Web GUI with measurable evaluation and evidence capabilities.

## Already implemented baseline

According to `llm-wiki/CURRENT_STATE.md`, the repository already has:

- Spring Boot 4.1.0 / Java 21 backend
- provider-neutral `AgentSessionPort`
- Codex-backed adapter using `codex app-server` over JSONL stdio
- initialize / thread start-resume / turn start
- asynchronous turn execution
- browser PC/mobile control UI
- workspace allowlist and symlink/traversal protection
- session events/timeline
- read-only Git status/diff
- human approval gate for deployment
- structured workflow result marker
- CI coverage for session/workflow/App Server behavior

Therefore the next career-evidence milestone should **not** be “prove Codex can be called from a web app.” That is already implemented.

## Next vertical slice

### 1. Evaluation Contract

Every task can optionally include acceptance criteria.

Example logical contract:

```text
TaskRequest
- instruction
- workspaceId
- acceptanceCriteria[]
- verificationCommands[]
- browserChecks[]
```

The agent response is not considered success merely because the model says it is done.

The gateway should independently record:

- build result
- test result
- lint/static checks when configured
- browser/API smoke result when configured
- changed files
- final verification state

### 2. Evidence Bundle

For each bounded workflow/task, retain a structured evidence view such as:

```text
request
workspace
agent/thread id mapping
start/end timestamps
stage transitions
changed-file list
commands/tests executed through controlled interfaces
verification results
approval events
final status
```

Do not store secrets or raw sensitive credentials.

### 3. Event transport

Current browser polling is adequate for the existing slice. A later milestone may introduce SSE or WebSocket after OQ-003 is resolved.

The career-value goal is to demonstrate:

- durable agent lifecycle independent of browser connection
- observable tool/workflow events
- reconnectable user supervision

### 4. Persistence

Current gateway session/workflow mappings are in memory. A portfolio-grade next milestone should persist only the minimum state required to reconnect after gateway restart.

The persistence technology and deployment remain unresolved under the existing wiki rules. Do not invent a database topology before OQ-005 is resolved.

### 5. Runtime adapter portability

Keep the existing `AgentSessionPort` boundary.

Candidate future adapters:

- Codex App Server — current implemented primary runtime
- bounded Codex Exec — task/CI-oriented runtime where appropriate
- OpenClaw — optional gateway/runtime integration if it adds channel/browser/tool value
- other future coding-agent runtimes

OpenClaw must not become a hard dependency of the core gateway.

## “No model API key” scope

A personal/self-hosted deployment may use Codex authentication already supported by the Codex client/runtime rather than implementing direct model API calls in the application.

This does **not** mean:

- the agent is offline;
- model usage has no limits;
- a ChatGPT subscription is a generic free API backend for arbitrary third-party users.

Any broader multi-user/commercial authentication and provider-use model must be verified separately against current official provider documentation and product terms.

## Career evidence target

When this vertical slice is implemented and independently verified, the repository should support claims such as:

> Designed and implemented a self-hosted Java/Spring control plane embedding the Codex agent harness, with isolated workspaces, asynchronous thread lifecycle, human approval gates, Git evidence, independent task verification, and reconnectable task history.

Claims about scale, production usage, uptime, or multi-user operation require runtime evidence and must not be inferred from architecture alone.

## Suggested milestone order

1. `M1` — acceptance criteria model + deterministic verification result
2. `M2` — evidence bundle API/UI
3. `M3` — persisted session/workflow mapping
4. `M4` — safe incremental SSE/WebSocket events after protocol decision
5. `M5` — optional second runtime adapter (OpenClaw or bounded Exec) to prove portability
6. `M6` — repeated benchmark tasks with success/failure/retry metrics

## Success criteria for portfolio use

At least 10 bounded tasks should be executed against disposable/test workspaces with recorded:

- requested outcome
- agent result
- independent verification result
- retry/failure classification
- elapsed time
- human approval count
- final Git diff/evidence

The goal is an auditable AI application system, not a collection of screenshots.

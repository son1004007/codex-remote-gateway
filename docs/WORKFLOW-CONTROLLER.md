# Workflow Controller

## Purpose

The workflow controller reduces the human control surface for Linux-based AI development work. A client such as mobile ChatGPT should normally need only `create`, `status`, `approve`, `resume`, and `cancel`.

The controller deliberately does not trust one AI to plan, implement, review, and declare its own work correct. Planning, implementation, adversarial review, and verification are separate stages with explicit handoff evidence.

## API

Base path: `/api/v1/workflows`

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/workflows` | Create and asynchronously start a workflow |
| `GET` | `/api/v1/workflows` | List workflow state |
| `GET` | `/api/v1/workflows/{id}` | Get detailed workflow state, per-stage outputs, workers, and events |
| `POST` | `/api/v1/workflows/{id}/approve` | Approve a workflow waiting at the deploy gate |
| `POST` | `/api/v1/workflows/{id}/resume` | Retry the current stage after `BLOCKED` or `FAILED` |
| `POST` | `/api/v1/workflows/{id}/cancel` | Stop further workflow progression and release workers best-effort |

`autoDeploy` defaults to `false`. Keep it false for normal operation.

## Collaboration policy

When Antigravity is enabled, the default assignment is:

| Stage | Primary | Purpose |
| --- | --- | --- |
| `PLAN` | Antigravity / Gemini | broad repository analysis, assumptions, risks, acceptance criteria, test and rollback strategy |
| `PLAN_VERIFY` | Codex | independently verify the plan against the real repository and correct unsupported assumptions |
| `IMPLEMENT` | Codex | implement the verified plan in the real workspace |
| `TEST_DESIGN` | Antigravity / Gemini | adversarial boundary, negative, regression, security, concurrency, and failure-case design |
| `TEST` | Codex | turn useful test ideas into executable checks, run them, reproduce failures, and fix validated defects |
| `REVIEW` | Antigravity / Gemini | independent read-only review for requirement gaps and defect candidates |
| `REVIEW_VERIFY` | Codex | reproduce review findings, reject false positives, fix validated defects, rerun targeted checks |
| `DEPLOY` | Codex for now | execute only repository-defined deployment after the human gate |
| `E2E` | Codex for now | execute repository-defined smoke/E2E evidence |

`DEPLOY` and `E2E` are intentionally still marked for replacement by a controlled deterministic runner.

If Antigravity is disabled or unavailable by configuration, analysis stages fall back to Codex so existing deployments continue to work.

## State machine

```text
READY
  -> PLAN                 (Gemini preferred)
  -> PLAN_VERIFY          (Codex)
  -> IMPLEMENT            (Codex)
  -> TEST_DESIGN          (Gemini preferred)
  -> TEST                 (Codex + deterministic tools)
  -> REVIEW               (Gemini preferred)
  -> REVIEW_VERIFY        (Codex + reproduction/tests)
  -> WAITING_APPROVAL     (DEPLOY, default)
  -> DEPLOY
  -> E2E
  -> COMPLETED
```

A plan is not accepted merely because a planner produced it. A review finding is not accepted merely because a reviewer reported it. `PLAN_VERIFY` and `REVIEW_VERIFY` must resolve those claims against repository or executable evidence.

## Explicit handoff

Each completed stage records:

- `stageWorkers`: which provider actually handled the stage;
- `stageOutputs`: bounded stage evidence passed to later stages;
- workflow events containing provider and stage result.

Later workers receive prior-stage evidence in their prompt. This avoids relying on hidden conversation memory when stages move between providers.

All AI output remains untrusted input. The controller requires a final marker:

```text
WORKFLOW_RESULT: SUCCESS
WORKFLOW_RESULT: BLOCKED - <reason>
WORKFLOW_RESULT: FAILED - <reason>
```

Missing markers fail closed as `BLOCKED`.

## Antigravity safety boundary

Antigravity is used only for `PLAN`, `TEST_DESIGN`, and `REVIEW` in this slice.

It does not receive the mutable real workspace. The gateway:

1. resolves the registered workspace using the same direct-child/real-path restrictions used by the Codex deployment model;
2. creates a disposable analysis snapshot excluding `.git`, build outputs, IDE metadata, virtual environments, and symlinks;
3. adds bounded read-only `git status` and working-tree diff context to the prompt;
4. runs `agy` in non-interactive `--prompt` mode with `--sandbox`;
5. deletes the temporary snapshot best-effort after the stage.

This means accidental Gemini edits affect only the disposable snapshot, not the real checkout.

Enable collaboration only after Antigravity CLI is installed and authenticated on the Linux runtime:

```text
GATEWAY_AGENT_MODE=codex
GATEWAY_ANTIGRAVITY_ENABLED=true
GATEWAY_ANTIGRAVITY_COMMAND=agy
GATEWAY_ANTIGRAVITY_MODEL=
GATEWAY_ANTIGRAVITY_TURN_TIMEOUT=5m
```

Leaving `GATEWAY_ANTIGRAVITY_MODEL` blank lets the installed CLI use its configured/default model. Pin a model only after it has been verified on the target host.

## Human gates

Normal human intervention is intentionally limited to:

1. a genuine unresolved requirement or environment decision reported as `BLOCKED`;
2. a failed execution that cannot be corrected safely from evidence;
3. deployment approval when `autoDeploy=false`.

Plan verification and review verification are machine-to-machine gates; they should not create routine mobile approval prompts.

## Known limitations

- Workflow/task state is still in memory and is lost on gateway restart.
- There is no GitHub Issue/PR state adapter yet.
- Antigravity authentication and installation are runtime prerequisites and are not bundled into the UBI image yet.
- Antigravity analysis is stateless per stage; handoff is through explicit workflow evidence rather than an Antigravity conversation ID.
- Existing Codex cancellation cannot yet guarantee interruption of a currently executing Codex turn.
- `DEPLOY` and `E2E` still use the AI worker instead of a dedicated controlled runner.
- HTTP authentication is not implemented; keep the service loopback/private-network protected.
- One active workflow per workspace remains intentional.

## Next slice

1. replace AI-owned `DEPLOY`/`E2E` with a controlled runner and evidence parser;
2. persist workflow snapshots, stage evidence, and provider assignments in PostgreSQL;
3. add GitHub Issue/PR/check adapters as durable task/change ledgers;
4. add provider health/preflight so a failed Antigravity runtime can be surfaced before a workflow starts;
5. measure Gemini review true-positive/false-positive rates from `REVIEW` versus `REVIEW_VERIFY` results.

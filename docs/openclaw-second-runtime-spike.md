# OpenClaw Second Runtime Spike

## Goal

Prove that `codex-remote-gateway` is not coupled to one agent harness by running the same bounded task contract through a second runtime path.

The existing primary runtime remains the direct Codex App Server adapter.

```text
AgentSessionPort
  |- CodexAgentSessionAdapter   (existing / primary)
  `- OpenClawAgentSessionAdapter (spike / optional)
```

OpenClaw is a portability experiment, not a mandatory dependency.

## Why this matters

The career/architecture value is not "using OpenClaw" itself. The value is demonstrating:

- provider/runtime abstraction
- session lifecycle ownership
- workspace boundary
- tool execution policy
- deterministic verification
- evidence bundle generation
- approval/security controls
- runtime portability

## Verified external capability

OpenClaw official documentation currently describes:

- an official Codex plugin that integrates with Codex App Server;
- reuse of an existing Codex CLI login during onboarding;
- native Codex thread/session supervision;
- ACP support for additional external coding harnesses.

References:

- https://docs.openclaw.ai/plugins/codex-harness
- https://docs.openclaw.ai/start/getting-started
- https://docs.openclaw.ai/tools/acp-agents

This validates feasibility only. It does not mean this repository already has an OpenClaw adapter.

## Important terminology

"Without a model API" means:

> the application does not directly hold an OpenAI model API key and call a model endpoint itself; instead it delegates execution to an already authenticated local/self-hosted agent runtime such as Codex App Server.

It does **not** mean:

- no internal API/protocol exists;
- offline/free/unlimited inference;
- a personal subscription can automatically be used as a generic backend for arbitrary third-party SaaS users.

## Phase 0 — do not start with OpenClaw

Issue #9 remains first priority.

Before adding another adapter, the existing Codex path must produce a portfolio-grade evidence bundle:

1. task request + acceptance criteria
2. agent result
3. changed files / Git diff
4. deterministic build/test result
5. approval events
6. final verified status
7. benchmark across at least 10 disposable tasks

Otherwise a second runtime only creates another chat/execution integration without stronger evidence.

## Phase 1 — runtime-neutral contract check

Review `AgentSessionPort` and identify whether any method leaks Codex-specific concepts.

Acceptance:

- no Codex thread-id type exposed in the port contract;
- runtime-specific metadata stays inside adapter/session metadata;
- existing Codex tests remain unchanged or require only neutral contract changes.

## Phase 2 — OpenClaw adapter spike

Implement behind a feature flag, disabled by default.

Suggested configuration shape:

```yaml
gateway:
  agent-runtime: codex-direct # codex-direct | openclaw
  openclaw:
    enabled: false
    command: openclaw
    timeout: 5m
```

The exact OpenClaw invocation must be based on the version installed on the runtime host; do not invent an unsupported CLI contract in code.

Required adapter behavior:

- start/bind a bounded session
- submit one turn
- capture assistant/result events
- cancel/stop best-effort
- map runtime errors to gateway-neutral errors
- enforce workspace boundary outside the runtime adapter as well

## Phase 3 — same acceptance suite

Run identical disposable tasks through:

```text
codex-direct
openclaw
```

Capture:

- task success/failure
- deterministic verifier result
- elapsed time
- retry count
- cancellation behavior
- changed files
- evidence bundle completeness

Do not compare model intelligence from a tiny sample. The target is runtime integration reliability and portability.

## Non-goals

- public multi-user SaaS
- bypassing provider billing/usage policy
- replacing deterministic tests with model self-evaluation
- unrestricted shell exposure
- making OpenClaw required for the existing direct Codex product

## Career evidence produced

Completion upgrades the portfolio claim from:

> Built a web UI around Codex.

into:

> Designed and implemented a runtime-neutral agent application backend with pluggable Codex/OpenClaw execution, deterministic verification, evidence capture, workspace isolation and human approval controls.

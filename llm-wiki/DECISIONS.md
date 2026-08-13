# Decisions

This file records durable decisions. When a decision changes, add a superseding entry instead of silently deleting history.

## D-001: Self-hosted Linux control plane

Status: `CONFIRMED`

The product will primarily target a self-hosted Linux environment that is remotely controlled through a browser.

Rationale:

- the main problem is remote supervision of coding-agent work on Linux;
- browser access reduces dependence on SSH or local desktop applications;
- self-hosting preserves control over workspaces and execution tools.

## D-002: Web UI is a control plane, not a raw shell

Status: `CONFIRMED`

The browser must not simply expose an unrestricted host shell. Session operations, command execution, approvals, Git inspection, and workspace access must pass through controlled service boundaries.

## D-003: Spring Boot for the control-plane backend

Status: `CONFIRMED`

Spring Boot is the planned backend foundation for authentication, persistence, session state, streaming endpoints, AI gateway integration, and operational APIs.

## D-004: Codex integration behind an adapter

Status: `CONFIRMED`

Codex-specific session/runtime integration must be isolated behind an adapter/service boundary rather than spread through controllers and UI code.

The exact transport and protocol must be verified against the supported Codex interface at implementation time.

## D-005: AI Gateway is provider-neutral

Status: `CONFIRMED`

Generic model calls belong behind an AI Gateway abstraction. Initial target providers are OpenAI/Codex-compatible access, Gemini, and Groq.

Vendor-specific SDK details must not leak into application controllers or the web UI.

## D-006: Explicit provider selection first

Status: `CONFIRMED`

The first gateway implementation should use explicit provider/model selection. Automatic routing/fallback based on cost, latency, or capability may be added later after measurable requirements exist.

## D-007: Human approval for high-impact operations

Status: `CONFIRMED`

Credential use, deployment, destructive Git operations, host administration, and other high-impact operations require approval or must be denied by policy.

## D-008: Git is the primary change ledger

Status: `CONFIRMED`

Agent-generated source modifications should remain observable through Git status, diff, branches, commits, and pull requests.

## D-009: Isolation evolves incrementally

Status: `CONFIRMED`

Implementation may begin with a dedicated Linux user and strict workspace allowlist, then evolve toward Docker-based per-session or per-workspace runners.

Unrestricted root execution is not an acceptable baseline.

## D-010: Multi-agent orchestration is post-MVP

Status: `CONFIRMED`

The first usable system will prove one workspace and one Codex session before adding multi-agent task graphs, shared artifacts, parallel execution, or orchestration workflows.

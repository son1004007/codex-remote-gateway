# Known Issues

This file tracks known defects, architectural risks, and technical debt. It is not a feature backlog.

## KI-001: Planning-only repository statement

Status: `DEPRECATED`

The original planning-only statement is obsolete. The repository now contains a Spring Boot control-plane slice, session adapters, a Codex App Server client, container/deployment automation, and regression tests. It must not be used to determine current implementation status.

See `CURRENT_STATE.md` and the source/test tree for the current evidence. Remote server validation remains separate and must be proven by host command output.

## KI-002: Codex integration assumptions can become stale

Status: `CONFIRMED`

Codex CLI/App Server behavior, authentication, protocols, and supported deployment modes may change over time.

Mitigation:

- verify current official documentation before implementation or migration;
- keep integration behind an adapter;
- capture verified protocol assumptions in an ADR or decision update.

## KI-003: Remote coding agents create a high-impact security boundary

Status: `CONFIRMED`

A compromised control plane or over-privileged runner could modify source, access credentials, execute commands, or damage repositories.

Mitigation is documented in `docs/SECURITY-DESIGN.md` and `llm-wiki/CONSTRAINTS.md`.

## KI-004: Multi-agent shared working tree would create race conditions

Status: `CONFIRMED`

Future parallel coding agents must not freely mutate one shared working tree. Use isolated worktrees/branches or another explicit isolation scheme before enabling bounded parallel execution.

## KI-005: Documentation can drift from implementation

Status: `CONFIRMED`

The wiki reduces but does not eliminate drift.

Mitigation:

- update `CURRENT_STATE.md` with implementation changes;
- prefer code/tests over stale prose when resolving conflicts;
- document discrepancies immediately when discovered.

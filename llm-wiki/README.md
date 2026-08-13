# LLM Wiki

This directory is the repository's compact, durable context for AI agents and humans.

Its purpose is to reduce repeated mistakes caused by stale conversation history, hidden assumptions, and confusion between plans and implemented behavior.

## Read order

1. `CURRENT_STATE.md` — what is actually implemented now
2. `DECISIONS.md` — durable decisions already made
3. `CONSTRAINTS.md` — requirements that must not be violated
4. `OPEN_QUESTIONS.md` — unresolved items that must not be guessed
5. `KNOWN_ISSUES.md` — known defects, risks, or technical debt
6. `ARCHITECTURE_MAP.md` — compact map of components and responsibilities
7. `WORK_LOG.md` — major historical milestones only

## Status vocabulary

Use these words consistently:

- `CONFIRMED`: verified by source, configuration, test, or explicit decision.
- `IMPLEMENTED`: present in current code/configuration.
- `PLANNED`: accepted direction but not yet implemented.
- `PROPOSED`: candidate idea that is not yet a decision.
- `BLOCKED`: cannot proceed until a dependency or decision is resolved.
- `UNKNOWN`: insufficient evidence; do not guess.
- `DEPRECATED`: intentionally superseded; do not revive without a new decision.

## Maintenance rules

- Keep files concise and factual.
- Prefer links to canonical repository docs rather than duplicating long explanations.
- Update wiki state in the same change that changes implementation or architecture.
- Never record secrets or private credentials.
- Do not use `WORK_LOG.md` as the current source of truth.
- When a decision changes, mark the previous decision as superseded instead of silently rewriting history.
- When implementation and documentation conflict, inspect executable code/configuration and tests first.

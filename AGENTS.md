# AGENTS.md

This repository is intended to be worked on repeatedly by humans and AI coding agents.

Before making changes, read these files in order:

1. `llm-wiki/README.md`
2. `llm-wiki/CURRENT_STATE.md`
3. `llm-wiki/DECISIONS.md`
4. `llm-wiki/CONSTRAINTS.md`
5. `llm-wiki/OPEN_QUESTIONS.md`

Then inspect the relevant source code and existing docs before modifying anything.

## Rules

- Treat repository code and configuration as implementation truth.
- Treat `CURRENT_STATE.md` as a concise snapshot, not a substitute for code inspection.
- Do not treat items in `OPEN_QUESTIONS.md` as confirmed facts.
- Do not invent ports, credentials, API quotas, provider behavior, deployment state, or completed work.
- Distinguish clearly between `confirmed`, `planned`, `proposed`, `blocked`, and `unknown`.
- If current implementation conflicts with the wiki, verify the code first, then update the wiki in the same change.
- Record durable architecture/product decisions in `DECISIONS.md`.
- Record new hard constraints in `CONSTRAINTS.md`.
- Add unresolved material questions to `OPEN_QUESTIONS.md` instead of guessing.
- Update `CURRENT_STATE.md` whenever implementation status materially changes.
- Update `WORK_LOG.md` for meaningful milestones, migrations, reversals, or incidents; do not log every trivial edit.
- Never put secrets, private credentials, private repository contents, or personal access tokens into the wiki.
- Prefer small, reviewable, testable changes over broad speculative rewrites.
- Preserve the security boundaries documented in `docs/SECURITY-DESIGN.md`.
- Do not silently weaken approval, authentication, workspace isolation, Git safety, or auditability.

## Priority of evidence

When sources disagree, use this order:

1. running behavior and tests;
2. current source/configuration;
3. migrations and executable infrastructure definitions;
4. `llm-wiki/CURRENT_STATE.md` and `llm-wiki/DECISIONS.md`;
5. architecture/product docs;
6. old work logs or historical discussion.

Document the discrepancy when it could mislead future agents.

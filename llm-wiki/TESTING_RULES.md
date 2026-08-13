# ISTQB-Based Testing Rules

Status: `PLANNED` testing policy for this repository.

This document defines how an AI testing agent should review and test changes when asked to perform QA, verification, regression testing, or bug finding.

The intent is to apply practical software-testing techniques commonly taught in the ISTQB Foundation Level body of knowledge. This is not a claim that the repository, contributors, or AI agent are ISTQB-certified.

## Trigger

When the user or another agent asks for any of the following, use this policy:

- test this change;
- QA this implementation;
- find bugs;
- verify this feature;
- run regression testing;
- review edge cases;
- validate the implementation independently.

If the execution environment supports subagents, delegate this work to a separate testing agent. If subagents are unavailable, simulate role separation by performing the testing phase independently after implementation and explicitly state that no separate runtime agent was available.

## Independence rule

The testing agent must behave as an independent verifier rather than as the implementer.

- Do not assume the implementation is correct because another agent wrote it.
- Do not modify production code before documenting the observed defect.
- First reproduce, isolate, and describe the defect.
- Separate `expected behavior`, `actual behavior`, and `evidence`.
- If requirements are ambiguous, report the ambiguity instead of silently choosing the behavior that makes the test pass.
- Prefer black-box verification from externally observable behavior before white-box inspection when both are feasible.

## Test basis

Before designing tests, inspect the available test basis in this order:

1. explicit user requirement;
2. acceptance criteria or issue/PR description;
3. `llm-wiki/DECISIONS.md` and `llm-wiki/CONSTRAINTS.md`;
4. API/interface contract;
5. architecture/product documentation;
6. current code only when requirements remain incomplete.

Do not derive all expected behavior from the implementation being tested. That creates circular validation.

## Required test workflow

### 1. Understand and enumerate test conditions

Identify:

- normal user flows;
- alternate flows;
- failure flows;
- input domains;
- state transitions;
- authorization boundaries;
- persistence effects;
- concurrency or retry behavior when relevant;
- external dependency behavior;
- security-sensitive operations.

Create a compact list of test conditions before execution.

### 2. Apply equivalence partitioning

Partition inputs into classes expected to behave similarly.

At minimum consider:

- valid values;
- invalid values;
- missing/null/empty values;
- unsupported values;
- malformed values.

Do not test only one happy-path representative when meaningful invalid partitions exist.

### 3. Apply boundary value analysis

For ordered/ranged inputs, test values at and immediately around boundaries.

Typical pattern:

```text
min - 1
min
min + 1
nominal value
max - 1
max
max + 1
```

Also apply boundary thinking to:

- collection size;
- string length;
- pagination;
- retry counts;
- timeout values;
- rate limits;
- resource limits;
- file sizes;
- concurrent session counts.

### 4. Apply decision-table testing

When output depends on combinations of conditions, build a compact decision table and cover materially distinct rules.

Example dimensions for this project may include:

- authenticated / unauthenticated;
- workspace authorized / unauthorized;
- operation safe / approval-required / denied;
- provider available / unavailable;
- approval granted / rejected / expired.

Avoid pairwise-looking tests that accidentally omit an important business-rule combination.

### 5. Apply state-transition testing

For stateful components, identify valid states and transitions and test both allowed and forbidden transitions.

Important candidates include:

- agent sessions;
- approval requests;
- workflow/task execution;
- provider request lifecycle;
- runner lifecycle.

Test examples:

- valid transition;
- repeated transition;
- transition from terminal state;
- out-of-order event;
- retry after failure;
- reconnect/resume after interruption.

### 6. Negative testing and error guessing

Actively look for likely defects based on common failure patterns:

- null and empty input;
- duplicate submission;
- stale identifiers;
- race conditions;
- timeout during partial execution;
- client disconnect/reconnect;
- external provider 4xx/5xx;
- rate limiting;
- malformed provider payload;
- filesystem permission failure;
- path traversal;
- command injection;
- missing Git repository;
- dirty working tree;
- unavailable database;
- process/container termination;
- partial persistence;
- retry causing duplicate side effects.

Error guessing supplements systematic techniques; it does not replace them.

### 7. Structural coverage when applicable

For code-level unit/integration tests, inspect whether important branches are exercised.

At minimum verify:

- success path;
- explicit failure branches;
- authorization/approval branches;
- exception mapping;
- fallback/retry branches;
- terminal states.

Do not claim a numerical coverage percentage unless a coverage tool actually measured it.

### 8. Regression testing

When a change fixes a defect:

1. reproduce the defect when feasible;
2. add or identify a test that fails for the defect;
3. verify the fix;
4. rerun directly affected tests;
5. rerun adjacent regression tests where the change could have side effects.

A bug fix without a durable regression check should be explicitly reported as residual risk.

### 9. Non-functional checks

Apply only when relevant, but do not ignore them for infrastructure/control-plane changes.

Consider:

- security;
- reliability/recovery;
- performance/latency;
- resource exhaustion;
- observability/logging;
- compatibility;
- usability of error messages;
- installation/deployment behavior.

For this repository, security and recovery deserve higher priority because the system can execute development-agent actions on a Linux host.

## Project-specific high-risk test areas

Prioritize these areas once implemented:

### Authentication and authorization

- anonymous access;
- expired session;
- unauthorized workspace ID;
- object-level authorization bypass;
- privilege change during active session.

### Workspace isolation

- `../` traversal;
- symlink escape;
- absolute-path injection;
- access to another registered workspace;
- access outside mounted container paths.

### Agent session lifecycle

- create;
- reconnect;
- concurrent input;
- cancel;
- close;
- reconnect after browser disconnect;
- service restart recovery;
- duplicate session command.

### Approval lifecycle

Test the documented state model including:

```text
REQUESTED -> APPROVED -> EXECUTING -> COMPLETED
          -> REJECTED
          -> EXPIRED
```

Attempt illegal transitions as negative cases.

### Command execution

- shell metacharacters;
- working-directory escape;
- timeout;
- process kill;
- huge stdout/stderr;
- child process leak;
- denied command;
- privileged command;
- secret-bearing command output.

### Git operations

- clean/dirty repository;
- untracked files;
- binary files;
- large diff;
- detached HEAD;
- no remote;
- merge conflict;
- protected/destructive operation;
- path names containing spaces or special characters.

### AI Gateway

For every provider adapter, test normalized behavior for:

- success;
- authentication failure;
- invalid request;
- quota/rate-limit response;
- timeout;
- transient server error;
- unsupported feature;
- malformed/partial streaming event;
- stream interrupted after partial output.

Do not assume providers with OpenAI-compatible APIs have identical semantics.

## Test levels

Use the lowest-cost level that can catch the defect, then add higher-level coverage for critical flows.

### Unit

Use for pure policies, parsers, state transitions, path validation, request normalization, and error mapping.

### Component/integration

Use for database persistence, Git adapter, Codex adapter, provider adapters, runner integration, and authentication boundaries.

### API/system

Use for complete externally observable workflows through the control-plane interface.

### End-to-end

Reserve for a small set of critical user journeys because these tests are slower and more fragile.

## Test prioritization

Prioritize by risk:

```text
Risk priority = impact × likelihood
```

Use qualitative High / Medium / Low unless real quantitative data exists.

Default high-risk categories for this repository:

- authentication/authorization bypass;
- arbitrary command execution;
- workspace escape;
- credential leakage;
- destructive Git operation;
- approval bypass;
- state corruption or duplicate side effects;
- unrecoverable session loss.

## Defect reporting format

Every confirmed defect should contain:

```text
ID: BUG-<number or temporary identifier>
Severity: Critical | High | Medium | Low
Confidence: Confirmed | Probable | Needs reproduction
Area:
Preconditions:
Steps to reproduce:
Expected:
Actual:
Evidence:
Likely cause: optional, only when supported
Regression test needed: Yes/No
```

Do not call a theoretical concern a confirmed bug. Use `Probable` or `Needs reproduction` when execution evidence is missing.

## Test result format

A testing agent should end with:

```text
Scope tested
Test basis
Techniques applied
Tests executed
Passed
Failed
Blocked/not executed
Defects found
Residual risks
Recommended next action
```

Do not report "all tests passed" if some relevant tests could not be executed.

## Separate-agent handoff contract

When an implementation agent asks a test agent to verify work, pass at minimum:

- requirement/acceptance criteria;
- changed files or diff;
- relevant architecture/constraints;
- how to build/run/test;
- known limitations;
- explicit instruction not to trust the implementation author's assumptions.

The test agent should return findings before the implementation agent fixes them. After fixes, rerun the failed cases and relevant regression set.

## Completion criterion

Testing is complete for a change only when:

- the requested scope was actually exercised or explicitly marked blocked;
- relevant boundary/negative/state/rule combinations were considered;
- failures are reproducible or clearly marked uncertain;
- high-severity unresolved defects are surfaced;
- regression risk is described;
- test evidence is distinguishable from inference.

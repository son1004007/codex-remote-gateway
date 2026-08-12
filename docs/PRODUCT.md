# Product Plan

## Problem

Codex and similar coding agents are powerful on a developer workstation, but long-running or remotely hosted development workflows need more than terminal access. A practical remote environment needs session control, observable execution, approval gates, Git review, isolation, and provider-independent AI access.

## Product objective

Build a self-hosted web application that lets a developer operate Codex-oriented development sessions on a Linux server safely from another device.

The long-term objective is an Agent Control Plane that can coordinate multiple specialized agents and AI providers while preserving human control over high-impact actions.

## Primary user

A software developer who:

- owns or controls a Linux development server;
- wants to continue development from a browser or mobile device;
- uses Codex as the main coding agent;
- may use Gemini, Groq, or other models for complementary tasks;
- wants Git-based traceability instead of opaque autonomous changes.

## User journeys

### Remote development

1. Sign in to the web control plane.
2. Select a registered workspace.
3. Start or reconnect to a Codex session.
4. Submit a development request.
5. Observe streamed reasoning-safe events, command progress, and outputs.
6. Approve or reject gated operations.
7. Inspect changed files and Git diff.
8. Ask the agent to test, revise, commit, or prepare a PR.

### Provider-assisted task

1. A workflow requests an AI capability.
2. The control plane sends the request through the AI gateway.
3. Gateway policy selects an explicitly configured provider/model.
4. Usage, latency, errors, and execution metadata are recorded.
5. The result returns to the calling workflow without provider-specific logic leaking into the UI.

### Future multi-agent workflow

1. Define a workflow composed of roles/tasks.
2. Declare dependencies between tasks.
3. Run independent tasks in parallel when safe.
4. Pass explicit artifacts between dependent tasks.
5. Pause for human approval at policy boundaries.
6. Resume failed or interrupted workflows from persisted state.

## MVP features

### Must have

- authenticated web access;
- workspace registry;
- Codex session start/reconnect/stop;
- prompt submission;
- server-sent or WebSocket streaming;
- execution event timeline;
- approval queue;
- Git status and unified diff;
- persisted session metadata;
- explicit error states;
- host-side configuration outside source control.

### Should have

- containerized execution;
- terminal-like command output without exposing an unrestricted browser shell;
- provider configuration;
- model selection;
- retry and timeout policies;
- resource and usage telemetry.

### Not in the first MVP

- fully autonomous production deployment;
- arbitrary public multi-tenancy;
- marketplace/plugin ecosystem;
- complex DAG editor;
- unattended access to unrestricted host credentials;
- automatic merging of agent-generated code.

## Success criteria

The MVP is successful when a developer can leave a Codex task running on a Linux host, reconnect through a browser, safely respond to approvals, inspect resulting Git changes, and continue the same session without direct SSH interaction for the normal workflow.

## Non-functional requirements

- Security boundaries must be explicit and documented.
- A disconnected browser must not terminate the underlying agent session.
- Server restart should preserve recoverable session/workflow metadata.
- Provider credentials must never be returned to the browser.
- Audit records must distinguish user requests, agent actions, approvals, and system events.
- The UI must make destructive or privileged actions visually distinguishable from normal progress.

# Architecture

## Context

The system is a self-hosted control plane around development agents. The browser is a management interface, not the execution environment. Agent processes run on a controlled Linux host or isolated runner.

## Logical components

### 1. Web UI

Responsibilities:

- authentication flow;
- workspace/session selection;
- prompt submission;
- live event rendering;
- approval decisions;
- Git status/diff presentation;
- workflow visualization in later phases.

The UI must never receive provider API keys or unrestricted host credentials.

### 2. Control Plane API

Responsibilities:

- authorization;
- workspace lifecycle;
- session lifecycle;
- event persistence and streaming;
- approval state machine;
- Git inspection API;
- audit records;
- orchestration state.

A Spring Boot service is a suitable implementation choice where strong typing, security integration, persistence, and long-running service operation are desirable.

### 3. Codex adapter / App Server integration

Codex integration should be isolated behind an adapter rather than embedded throughout controllers and UI code.

Conceptual interface:

```text
AgentSessionService
  createSession(workspace)
  reconnect(sessionId)
  submit(sessionId, input)
  cancel(sessionId)
  approve(sessionId, approvalId, decision)
  streamEvents(sessionId)
  close(sessionId)
```

The exact transport must follow the supported Codex/App Server interface available at implementation time.

### 4. AI Gateway

The gateway handles model-provider integrations that are not intrinsic to the Codex session itself.

Conceptual interface:

```text
AiProvider
  id()
  capabilities()
  execute(request)
  stream(request)
```

Initial adapters:

- OpenAI/Codex-compatible
- Gemini
- Groq

Provider selection should be policy/configuration driven. Application features should request capabilities rather than hard-code vendor SDK calls.

### 5. Workspace service

Each workspace maps to an explicitly configured repository/directory. A workspace record should include:

- ID and display name;
- canonical filesystem path;
- repository remote metadata;
- allowed runner profile;
- environment/secrets references;
- policy profile;
- active session metadata.

Never accept an arbitrary browser-provided host path as trusted input.

### 6. Runner

The runner executes agent commands and project tools.

Preferred evolution:

1. MVP: dedicated Linux user with strict workspace allowlist.
2. Next: Docker container per session/workspace.
3. Later: pluggable execution backend.

Container boundaries should cover filesystem mounts, CPU/memory/PID limits, network policy, user identity, and secrets exposure.

### 7. Persistence

Recommended entities:

- User
- Workspace
- AgentSession
- SessionEvent
- ApprovalRequest
- ApprovalDecision
- ExecutionRecord
- ProviderRequest
- Workflow
- WorkflowRun
- WorkflowTaskRun

PostgreSQL is appropriate for durable control-plane state. High-volume raw output may later move to object/file storage while retaining indexed metadata in PostgreSQL.

## Event model

Use an append-oriented event model for observable execution.

Example event types:

```text
SESSION_STARTED
USER_INPUT
AGENT_MESSAGE
COMMAND_REQUESTED
APPROVAL_REQUIRED
APPROVAL_GRANTED
APPROVAL_REJECTED
COMMAND_STARTED
COMMAND_OUTPUT
COMMAND_FINISHED
FILE_CHANGED
GIT_STATUS_CHANGED
SESSION_FAILED
SESSION_COMPLETED
```

The browser subscribes to events by session ID. WebSocket is appropriate for bidirectional interactive control; SSE is sufficient if command/control requests remain normal HTTP calls.

## Approval state machine

```text
REQUESTED -> APPROVED -> EXECUTING -> COMPLETED
          -> REJECTED
          -> EXPIRED
```

Approval records should capture the requested operation, risk category, requesting session, user decision, timestamp, and execution result.

## Git integration

The control plane should expose safe read operations first:

- repository status;
- changed file list;
- unified diff;
- current branch;
- recent commits.

Write operations such as commit, push, branch deletion, reset, force push, or merge require explicit policy and, where appropriate, approval.

## Deployment topology

Recommended initial deployment:

```text
Internet/VPN
    |
Reverse Proxy / TLS
    |
Control Plane
    |---- PostgreSQL
    |---- Codex adapter
    |---- AI Gateway
    |
Runner boundary
    |
Registered Git workspaces
```

Prefer VPN/private access for the first deployment. If exposed publicly, add hardened authentication, MFA/SSO where possible, rate limiting, CSRF protection where applicable, secure cookies, strict origin rules, and reverse-proxy controls.

## Multi-agent evolution

Do not start by giving several agents unrestricted access to the same working tree. A safer orchestration model is:

- workflow has explicit tasks;
- each task has role, input, output contract, and dependencies;
- parallel tasks use separate worktrees/branches or isolated copies;
- artifacts are handed off explicitly;
- integration occurs through a designated task;
- state is persisted so execution can resume.

Example:

```text
Requirements Agent
      |
      v
Architecture Agent
      |
  +---+---+
  v       v
Backend  Test Agent
  |       |
  +---+---+
      v
Review/Integration Agent
      |
      v
Human Approval
```

This provides a path from a remote Codex UI to a real Agent Control Plane without making the MVP dependent on complex orchestration.

# Security Design

## Security objective

The project intentionally controls software-development agents capable of executing commands and modifying source code. Treat the runner as a privileged automation boundary, not as a normal chat application.

## Primary threats

### Remote control exposure

An attacker who gains access to the control plane could potentially modify source, run commands, or extract credentials.

Controls:

- private/VPN deployment preferred initially;
- TLS only;
- strong authentication;
- short-lived authenticated sessions;
- authorization per workspace;
- rate limiting and lockout controls;
- audit login and control actions.

### Arbitrary host command execution

Do not expose an unrestricted web shell as the core design.

Controls:

- agent/runner adapter mediates execution;
- explicit workspace allowlist;
- non-root service account;
- container isolation where practical;
- command policy and approval categories;
- resource limits;
- no host Docker socket mounted into untrusted runners.

### Prompt/tool injection

Repository files, web content, dependency output, or generated text may contain instructions attempting to influence an agent.

Controls:

- external content is data, not trusted policy;
- system policies are not sourced from repository content;
- sensitive operations require policy checks independent of model output;
- approvals display the concrete operation rather than only an agent explanation.

### Secret leakage

Controls:

- secrets remain server-side;
- provider keys are referenced by configuration IDs;
- redact known secret patterns from persisted output where feasible;
- inject minimum required secrets into runners;
- never commit `.env` or production configuration;
- separate control-plane credentials from project credentials.

### Repository destruction

Risky examples:

- force push;
- reset/clean;
- deleting branches/tags;
- modifying protected configuration;
- mass file deletion.

Controls:

- classify Git write operations;
- require approval for destructive operations;
- preserve event/audit history;
- recommend protected default branches and PR-based integration.

### Cross-workspace access

Controls:

- canonicalize paths;
- map workspace IDs to server-controlled paths;
- reject path traversal;
- isolate filesystem mounts per runner;
- authorize every session against workspace ownership/permissions.

## Approval categories

Suggested baseline:

| Category | Example | Default |
|---|---|---|
| Read-only | inspect source, git diff | allow |
| Build/test | Maven/Gradle/npm test | allow by workspace policy |
| Dependency change | install/update packages | configurable |
| Source modification | normal project edits | allow within workspace |
| External network | arbitrary outbound request | configurable |
| Credential use | cloud/Git/provider credential | approval |
| Deployment | production/staging deployment | approval |
| Destructive Git | force push/reset/delete | approval or deny |
| Host administration | sudo/system service changes | deny by default |

## Audit requirements

Record at minimum:

- actor;
- workspace;
- session;
- event type;
- requested operation;
- approval decision;
- timestamp;
- execution status;
- relevant command metadata;
- resulting Git state when applicable.

Do not treat raw model output as an authoritative audit record. System-generated events should be distinguished from agent-generated text.

## Secure defaults

- bind application services to private interfaces where possible;
- terminate TLS at a hardened reverse proxy;
- disable anonymous workspace access;
- deny unknown workspaces;
- deny host-root execution;
- deny unrestricted secret enumeration;
- deny destructive operations unless explicitly enabled;
- set execution timeouts;
- set container resource limits;
- keep dependency and base-image versions maintained.

## Security review gates

Before public Internet exposure:

- authentication/authorization review;
- threat model update;
- dependency scan;
- secret scan;
- container escape/privilege review;
- CSRF/CORS/session review;
- rate-limit verification;
- audit-log verification;
- backup/recovery test.

Before unattended autonomous deployment capabilities are introduced, perform a separate threat model rather than treating deployment as an incremental UI feature.

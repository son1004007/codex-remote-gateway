# Deployment and Server Validation

This document defines the first server-testable milestone for Codex Remote Gateway. Infrastructure identifiers, IP addresses, usernames, and credential references must remain outside this public repository.

For the CI-gated two-server rollout used after normal development changes, also read `docs/AUTO-DEPLOY.md`.

## Target state

The first deployable milestone is intentionally internal-only:

```text
browser/admin machine
        |
        | SSH tunnel or host-local curl
        v
127.0.0.1:18080
        |
        v
Codex Remote Gateway (Spring Boot 4 / Java 21)
        |
        | spawn per turn
        v
codex app-server
        |
        | JSONL over stdio
        v
ChatGPT-authenticated Codex runtime
        |
        v
/workspaces/<workspaceId>
```

The application container uses a Red Hat UBI 9 / Node.js 22 base and installs OpenJDK 21 plus the Codex CLI. This allows the same image to be validated on older Ubuntu and Red Hat-family Docker hosts without depending on the host's Python, Node.js, Java, or glibc userland.

## Recommended validation order

### Stage 1 - CI and local container build

Gate:

- `mvn verify` passes.
- `Containerfile` builds.
- Application starts with `GATEWAY_AGENT_MODE=in-memory` in normal JVM tests.
- Container starts with `GATEWAY_AGENT_MODE=codex` and reports health `UP` before an authenticated Codex turn is attempted.

### Stage 2 - Red Hat-family Docker host

Use the Red Hat-family IDC Docker host as the primary compatibility target.

Reasons:

- validates the requested Red Hat-family deployment path;
- exercises Docker bind mounts and possible SELinux labeling;
- does not depend on NVIDIA/CUDA;
- Codex itself is remote-model backed, so GPU hardware is not used by this milestone.

Keep the service bound to loopback. Do not expose port 18080 externally yet.

### Stage 3 - Ubuntu GPU host

Use the Ubuntu GPU host as a second runtime target after Stage 2 succeeds.

The current gateway does not use the GPU. This host becomes strategically useful later when the AI Gateway adds a local provider such as Ollama/vLLM. For the current Codex-only milestone it is a cross-host compatibility test, not a GPU acceleration test.

Because older host OS, Docker, NVIDIA driver, and CUDA versions may exist, keep the Codex gateway isolated inside the UBI 9 container and do not change the GPU stack merely to deploy this milestone.

## First deployment

On the target host:

```bash
git clone https://github.com/son1004007/codex-remote-gateway.git
cd codex-remote-gateway
cp .env.example .env
bash scripts/preflight.sh
bash scripts/prepare-runtime.sh
```

For an SELinux-enforcing host, set this in `.env` if Docker bind mounts are denied:

```text
VOLUME_SUFFIX=:Z
```

Build the image:

```bash
docker compose build gateway
```

If only legacy Compose is installed, the helper scripts automatically use `docker-compose`.

## ChatGPT/Codex authentication

Authentication is deliberately performed after the image is built and before the service is considered Codex-ready.

Run:

```bash
bash scripts/codex-login.sh
```

The helper first runs:

```text
codex login status
```

When that succeeds, it preserves the existing persistent login and exits without device authentication. Only when the status check fails does it run:

```text
codex login --device-auth
```

inside the container while mounting the persistent Codex home directory. Complete the device flow in a browser on another trusted device when prompted.

Do not commit:

- `auth.json`;
- refresh/access tokens;
- one-time device codes;
- API keys;
- browser cookies.

The persistent host directory defaults to `runtime/codex-home` and is excluded from source control.

## Start the gateway

```bash
bash scripts/deploy-local.sh
```

Expected local endpoint:

```text
http://127.0.0.1:18080
```

Health check:

```bash
curl -sS http://127.0.0.1:18080/actuator/health
```

Expected result contains:

```json
{"status":"UP"}
```

## End-to-end Codex smoke test

After ChatGPT device authentication succeeds:

```bash
bash scripts/smoke-test.sh
```

The script performs:

1. Actuator health check.
2. Creates a gateway session using workspace `smoke`.
3. Submits a Codex turn.
4. Verifies that a Codex provider thread id was bound.
5. Verifies an assistant event was returned.
6. Verifies the response includes `gateway-ok`.
7. Submits a second turn to the same gateway session.
8. Verifies the same provider thread is resumed and the second expected response is returned.

Pass criterion:

```text
Smoke test: PASS
```

## Remote helper

Keep SSH aliases in the administrator's local `~/.ssh/config` or another private inventory. Then use:

```bash
bash scripts/remote-target.sh <ssh-alias> deploy
bash scripts/remote-target.sh <ssh-alias> login
bash scripts/remote-target.sh <ssh-alias> smoke
```

`login` uses `ssh -t` because device authentication is interactive. The safe validation sequence is:

```bash
bash scripts/remote-target.sh <ssh-alias> sync
bash scripts/remote-target.sh <ssh-alias> preflight
bash scripts/remote-target.sh <ssh-alias> deploy
bash scripts/remote-target.sh <ssh-alias> login
bash scripts/remote-target.sh <ssh-alias> smoke
```

`sync` creates a clone on a new path, or updates an existing checkout only when it is clean, on `main`, has the expected origin, and can fast-forward to `origin/main`. It never resets a remote working tree. If a remote `.env` does not yet exist, it is initialized from `.env.example`; lifecycle scripts load the documented `.env` variables without evaluating it as shell code, and explicitly exported values take precedence.

## Automatic rollout after AI-assisted changes

After both targets have completed first-time key and Codex authentication setup, normal rollout can use `.github/workflows/deploy.yml`.

The workflow does **not** deploy every commit to `main`. A complete change set is marked by a final update to `.deploy/trigger`. The corresponding CI run must succeed before deployment begins. This avoids deploying intermediate commits generated while ChatGPT/Codex is still modifying several files.

Deployment order is always:

```text
exact CI-tested commit
 -> primary server
 -> smoke test
 -> secondary server
 -> smoke test
```

Connection details and SSH private keys are provided through generic GitHub Environments and are not stored in this public repository. See `docs/AUTO-DEPLOY.md` for the complete setup and security model.

## Acceptance criteria for "server-testable"

A target is considered validated only when all conditions below are met:

- Docker daemon reachable.
- Compose available.
- At least 5 GiB deployment filesystem space available.
- UBI 9 gateway image builds successfully.
- `codex --version` succeeds inside the image.
- `codex login status` reports a ChatGPT-authenticated session.
- Gateway health is `UP`.
- `smoke` workspace is mounted and accepted.
- Session creation returns HTTP 201.
- First Codex turn completes without HTTP 502.
- Response contains a provider thread id and assistant message.
- A second message on the same gateway session resumes the same Codex thread and returns `gateway-resume-ok`.

## Rollback

The gateway currently stores HTTP/API session objects only in memory. Rollback is therefore simple:

```bash
docker compose down
```

To revert application code:

```bash
git fetch origin main
git checkout <known-good-commit>
docker compose build gateway
docker compose up -d gateway
```

Do not delete `runtime/codex-home` during ordinary rollback; it contains the persisted Codex authentication/session state. Delete it only when intentionally logging out or resetting Codex state.

## Current limitations

This milestone is not production-ready.

- Gateway API sessions are in memory and disappear when Spring Boot restarts.
- Codex threads persist under `CODEX_HOME`, but gateway-to-thread mapping is not yet persisted.
- M1 permits one active gateway session per workspace; cancel it before creating another session for the same workspace.
- Each submitted gateway turn starts a fresh `codex app-server` process, then resumes or starts the stored Codex thread and terminates the process after `turn/completed`.
- HTTP submission is synchronous; SSE/WebSocket streaming is not implemented yet.
- `cancel` changes gateway session state between turns; active `turn/interrupt` wiring is not implemented yet.
- Unexpected server-initiated approval requests are declined by the M1 client. The default deployment also uses approval policy `never` with workspace-write sandboxing.
- Authentication/authorization for the gateway HTTP API is not implemented. Therefore the service must remain loopback-only or otherwise protected by a private network/SSH tunnel during this milestone.

## Next milestone after both servers pass

1. Persist gateway session to Codex thread mapping.
2. Keep managed App Server processes alive instead of one process per turn.
3. Implement event streaming to browser clients.
4. Implement `turn/interrupt` and approval UI.
5. Add gateway authentication and workspace ACLs.
6. Add reverse proxy/TLS only after gateway authentication exists.
7. Add a local GPU provider separately; do not couple local inference to the Codex adapter.

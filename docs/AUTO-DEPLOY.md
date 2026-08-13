# Automatic two-server deployment

This repository supports an opt-in deployment trigger for AI-assisted work on `main`.

The intended flow is:

```text
complete change set
 -> CI passes
 -> final .deploy/trigger update
 -> CI passes for that exact commit
 -> primary server deploy + smoke test
 -> secondary server deploy + smoke test
```

The deployment workflow is `.github/workflows/deploy.yml`.

## Why deployment is marker-gated

ChatGPT, Codex, GitHub APIs, and other automation may create several sequential commits while completing one logical task. Deploying every intermediate `main` commit could put a partially updated state on the servers.

Therefore normal source/doc commits do not deploy automatically. The final deployable state is explicitly marked by updating `.deploy/trigger` once after implementation and verification are complete.

The deployment workflow is launched after the repository `CI` workflow completes successfully. It then verifies that the CI-tested commit changed `.deploy/trigger`. The exact tested SHA is deployed rather than blindly pulling the latest `main` state.

## Required GitHub Environments

Create these two environments in repository settings:

```text
server-primary
server-secondary
```

Keep the names generic in this public repository. The private device inventory owns the mapping from those names to real infrastructure.

Each environment must contain the same secret names with values specific to that server:

```text
SSH_HOST
SSH_PORT
SSH_USER
SSH_PRIVATE_KEY
SSH_KNOWN_HOSTS
```

Optional environment variable:

```text
REMOTE_DIR
```

If `REMOTE_DIR` is absent, deployment uses:

```text
$HOME/codex-remote-gateway
```

### Secret meaning

- `SSH_HOST`: server address used by the GitHub-hosted runner.
- `SSH_PORT`: SSH TCP port.
- `SSH_USER`: dedicated deployment account where possible.
- `SSH_PRIVATE_KEY`: private half of the server-login key. Never commit this value.
- `SSH_KNOWN_HOSTS`: pinned OpenSSH known-hosts entry for the exact host/port. Do not generate this dynamically during deployment.

The corresponding `.pub` file may be kept in the private device inventory and must be registered in the target account's `authorized_keys`. The public key is not a substitute for `SSH_PRIVATE_KEY` inside the GitHub deployment job.

## Host-key pinning

Before enabling automatic deployment, collect and independently verify each server's SSH host-key fingerprint from a trusted channel. Store the resulting `known_hosts` line in the corresponding GitHub Environment secret `SSH_KNOWN_HOSTS`.

The workflow uses:

```text
StrictHostKeyChecking=yes
```

It intentionally does not accept a new host key automatically. A host-key mismatch must fail deployment and be investigated rather than bypassed.

For non-standard SSH ports, a known-hosts entry commonly identifies the endpoint in bracket form:

```text
[host]:port key-type public-host-key
```

Do not copy real infrastructure identifiers into this public document.

## First-time target preparation

Before relying on automatic deployment, each target must be prepared once:

1. Register the correct deployment public key in the target account's `authorized_keys`.
2. Confirm key-only login from an administrator machine.
3. Confirm Docker and Docker Compose requirements in `scripts/preflight.sh`.
4. Perform the Codex ChatGPT device login with `scripts/codex-login.sh`.
5. Keep `runtime/codex-home` persistent and excluded from Git.
6. Perform a manual smoke test once.

After this bootstrap, automatic deployments are non-interactive. If Codex authentication expires or is removed, the smoke test will fail and a trusted administrator must perform interactive login again.

## Deployment behavior

The deployment workflow runs on a GitHub-hosted runner and connects to each target through OpenSSH.

For each server it:

1. connects with `BatchMode=yes`, `IdentitiesOnly=yes`, and strict host-key checking;
2. clones the public repository if absent, otherwise fetches `origin/main`;
3. hard-resets the deployment checkout to the exact CI-tested SHA;
4. verifies `git rev-parse HEAD` equals that SHA;
5. runs `scripts/preflight.sh`;
6. runs `scripts/deploy-local.sh`;
7. runs `scripts/smoke-test.sh`.

The secondary server depends on successful deployment of the primary server. If primary deployment or smoke testing fails, secondary deployment does not start.

## Manual deployment workflow run

`Deploy test servers` also supports `workflow_dispatch`. A manual run deploys the selected current commit even when `.deploy/trigger` did not change. Use this for deliberate reconciliation or deployment-pipeline verification, not as the normal development path.

## Deployment protection

GitHub Environments can restrict deployment branches, hold secrets until environment rules pass, and optionally require reviewers. For the current personal test setup, an environment can be limited to `main` without a required reviewer to preserve automatic rollout.

If the servers later become production-like targets, enable required reviewers before allowing access to the environment secrets.

## Rollback

Automatic deployment always identifies the exact SHA. For rollback, first determine a known-good commit, test it, then explicitly deploy that commit through a controlled workflow or restore the marker to a commit containing the intended code state.

Do not delete persistent `runtime/codex-home` as part of normal rollback.

## Security boundaries

- Never store SSH private keys in source files, LLM Wiki, issues, workflow YAML, `.env.example`, or device documents.
- Never use password authentication from GitHub Actions.
- Never disable strict host-key checking to fix a deployment failure.
- Do not expose the gateway externally merely to simplify CI/CD.
- Keep the gateway loopback-bound until HTTP authentication and external transport security are implemented.
- A contributor able to change trusted deployment workflows can potentially influence how environment secrets are used. Protect `main` and review workflow changes carefully.

# Constraints

These constraints are mandatory unless explicitly superseded by a documented decision.

## Security

- Do not expose an unrestricted host shell as the product's primary interface.
- Do not run the control plane or runner as root by default.
- Do not mount the host Docker socket into an untrusted runner.
- Do not expose provider API keys, access tokens, or host credentials to the browser.
- Do not persist secrets in repository files, logs, or LLM Wiki documents.
- Do not bypass approval policy for destructive Git, deployment, credential, or host-administration operations.
- Workspace IDs must resolve to server-controlled canonical paths; never trust an arbitrary browser-provided filesystem path.

## Architecture

- Keep Codex runtime/session integration behind a dedicated adapter boundary.
- Keep generic model/provider integrations behind the AI Gateway.
- Do not couple UI code directly to provider SDKs.
- Preserve a durable session/event model so browser disconnects do not define agent lifecycle.
- Prefer explicit provider selection until automatic routing is justified by measured requirements.

## Delivery

- Implement the single-workspace/single-session vertical slice before multi-agent orchestration.
- Do not mark planned capabilities as implemented without source/configuration evidence.
- Add or update tests for behavior changes where practical.
- Keep implementation and wiki state synchronized in the same change.

## Documentation and AI behavior

- Never invent credentials, ports, quotas, rate limits, API behavior, or deployment state.
- For current external APIs/products, verify current official documentation before relying on behavior that may change.
- Treat unresolved questions as `UNKNOWN` or `PROPOSED`, not as facts.
- When code contradicts stale documentation, inspect executable behavior first and correct the documentation.

# Security Policy

## Reporting a vulnerability

Please do not publish exploit details or sensitive vulnerability information in a public issue before a fix or mitigation can be prepared.

For now, use GitHub's private vulnerability reporting/security advisory capability for this repository when available. If private reporting is not available, contact the repository owner privately before public disclosure.

Include:

- affected component/version or commit;
- reproduction conditions;
- security impact;
- proof of concept when safe to provide;
- suggested mitigation if known.

## Security-sensitive areas

Changes affecting the following require additional review:

- authentication or authorization;
- command execution;
- approval bypasses;
- filesystem/workspace isolation;
- container privileges;
- secrets and credentials;
- Git destructive operations;
- public network exposure;
- provider request logging;
- deployment automation.

See `docs/SECURITY-DESIGN.md` for the baseline threat model and controls.

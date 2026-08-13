#!/usr/bin/env bash
set -euo pipefail

workspace_root=${WORKSPACE_ROOT:-./runtime/workspaces}
codex_home=${CODEX_HOME_HOST:-./runtime/codex-home}

mkdir -p "$workspace_root/smoke" "$codex_home"
chmod 700 "$codex_home" || true

if [ ! -f "$workspace_root/smoke/README.md" ]; then
  cat > "$workspace_root/smoke/README.md" <<'EOF'
# Codex Remote Gateway smoke workspace

This directory exists only for deployment smoke tests.
EOF
fi

printf 'Workspace root: %s\n' "$(cd "$workspace_root" && pwd)"
printf 'Codex home: %s\n' "$(cd "$codex_home" && pwd)"

#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(cd "$(dirname "$0")/.." && pwd)
cd "$repo_dir"
# shellcheck disable=SC1091
source scripts/load-server-target.sh

load_server_target server-primary
[ "$TARGET_NAME" = 'onycom-rtx2080' ]
[ "$SSH_HOST" = '106.245.232.34' ]
[ "$SSH_PORT" = '55522' ]
[ "$SSH_USER" = 'sks88' ]
[ "$TARGET_RUNTIME" = 'host-local' ]

load_server_target server-secondary
[ "$TARGET_NAME" = 'idc-data1' ]
[ "$SSH_HOST" = '222.239.10.71' ]
[ "$SSH_PORT" = '10003' ]
[ "$SSH_USER" = 'root' ]
[ "$TARGET_RUNTIME" = 'compose' ]

if load_server_target unknown-target >/dev/null 2>&1; then
  echo 'unknown target unexpectedly accepted' >&2
  exit 1
fi

bad=$(mktemp)
trap 'rm -f "$bad"' EXIT
cat > "$bad" <<'EOF'
server-primary.name=test
server-primary.host=example.invalid
server-primary.port=not-a-port
server-primary.user=tester
server-primary.runtime=compose
EOF
if load_server_target server-primary "$bad" >/dev/null 2>&1; then
  echo 'invalid target config unexpectedly accepted' >&2
  exit 1
fi

printf 'Server target config tests: PASS\n'

#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(cd "$(dirname "$0")/.." && pwd)
cd "$repo_dir"
# shellcheck disable=SC1091
source scripts/load-server-target.sh

load_server_target office
[ "$TARGET_NAME" = 'office-rtx2080' ]
[ "$SSH_HOST" = '106.245.232.34' ]
[ "$SSH_PORT" = '55522' ]
[ "$SSH_USER" = 'sks88' ]
[ "$TARGET_RUNTIME" = 'host-local' ]

load_server_target idc
[ "$TARGET_NAME" = 'idc-data1' ]
[ "$SSH_HOST" = '222.239.10.71' ]
[ "$SSH_PORT" = '10003' ]
[ "$SSH_USER" = 'root' ]
[ "$TARGET_RUNTIME" = 'compose' ]

if load_server_target primary >/dev/null 2>&1; then
  echo 'legacy primary target unexpectedly accepted' >&2
  exit 1
fi
if load_server_target secondary >/dev/null 2>&1; then
  echo 'legacy secondary target unexpectedly accepted' >&2
  exit 1
fi

bad=$(mktemp)
trap 'rm -f "$bad"' EXIT
cat > "$bad" <<'EOF'
office.name=test
office.host=example.invalid
office.port=not-a-port
office.user=tester
office.runtime=compose
EOF
if load_server_target office "$bad" >/dev/null 2>&1; then
  echo 'invalid target config unexpectedly accepted' >&2
  exit 1
fi

printf 'Server target config tests: PASS\n'

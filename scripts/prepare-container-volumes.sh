#!/usr/bin/env bash
set -euo pipefail

if docker compose version >/dev/null 2>&1; then
  compose=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  compose=(docker-compose)
else
  echo "docker compose plugin or docker-compose is required" >&2
  exit 1
fi

"${compose[@]}" run --rm --user 0 --entrypoint /bin/sh gateway -c '
  set -e
  mkdir -p /opt/app-root/src/.codex /workspaces/smoke
  chown -R 1001:0 /opt/app-root/src/.codex /workspaces/smoke
  chmod 700 /opt/app-root/src/.codex
  chmod -R u+rwX,g+rX,o-rwx /workspaces/smoke
'

printf 'Container bind-mount permissions: PASS\n'

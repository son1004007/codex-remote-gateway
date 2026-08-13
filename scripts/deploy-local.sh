#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
repo_dir=$(cd "$script_dir/.." && pwd)
cd "$repo_dir"

bash "$script_dir/preflight.sh"
bash "$script_dir/prepare-runtime.sh"

if docker compose version >/dev/null 2>&1; then
  compose=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  compose=(docker-compose)
else
  echo "docker compose plugin or docker-compose is required" >&2
  exit 1
fi

"${compose[@]}" build gateway
"${compose[@]}" up -d gateway

container_id=$("${compose[@]}" ps -q gateway)
[ -n "$container_id" ] || { echo "gateway container was not created" >&2; exit 1; }

for _ in $(seq 1 30); do
  health=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container_id" 2>/dev/null || true)
  if [ "$health" = "healthy" ]; then
    printf 'Gateway health: PASS\n'
    printf 'Local endpoint: http://%s:%s\n' "${GATEWAY_BIND_ADDRESS:-127.0.0.1}" "${GATEWAY_PORT:-18080}"
    exit 0
  fi
  if [ "$health" = "unhealthy" ]; then
    "${compose[@]}" logs --tail=200 gateway >&2
    exit 1
  fi
  sleep 2
done

"${compose[@]}" logs --tail=200 gateway >&2
echo "gateway did not become healthy" >&2
exit 1

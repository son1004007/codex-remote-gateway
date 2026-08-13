#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
repo_dir=$(cd "$script_dir/.." && pwd)
cd "$repo_dir"

if docker compose version >/dev/null 2>&1; then
  compose=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  compose=(docker-compose)
else
  echo "docker compose plugin or docker-compose is required" >&2
  exit 1
fi

bash "$script_dir/prepare-runtime.sh"
"${compose[@]}" build gateway
bash "$script_dir/prepare-container-volumes.sh"

cat <<'EOF'
Starting Codex device-code login inside the persistent CODEX_HOME volume.
If device authorization is disabled in ChatGPT security settings, enable it first and retry.
Do not copy auth.json or device codes into this repository.
EOF

"${compose[@]}" run --rm --entrypoint codex gateway login --device-auth
"${compose[@]}" run --rm --entrypoint codex gateway login status

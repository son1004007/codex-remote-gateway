#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

command -v docker >/dev/null 2>&1 || fail "docker is required"
docker info >/dev/null 2>&1 || fail "docker daemon is not reachable"
command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v git >/dev/null 2>&1 || fail "git is required"

if docker compose version >/dev/null 2>&1; then
  compose_cmd='docker compose'
elif command -v docker-compose >/dev/null 2>&1; then
  compose_cmd='docker-compose'
else
  fail "docker compose plugin or docker-compose is required"
fi

printf 'Docker: %s\n' "$(docker --version)"
printf 'Compose: %s\n' "$($compose_cmd version 2>/dev/null | head -n 1)"
printf 'Kernel: %s\n' "$(uname -srmo)"
printf 'Architecture: %s\n' "$(uname -m)"

available_kb=$(df -Pk . | awk 'NR==2 {print $4}')
if [ "${available_kb:-0}" -lt 5242880 ]; then
  fail "at least 5 GiB free space is required in the deployment filesystem"
fi

if command -v getenforce >/dev/null 2>&1 && [ "$(getenforce 2>/dev/null || true)" = "Enforcing" ]; then
  if [ "${VOLUME_SUFFIX:-}" != ":Z" ] && [ "${VOLUME_SUFFIX:-}" != ":z" ]; then
    printf 'WARN: SELinux is enforcing. Set VOLUME_SUFFIX=:Z if bind mounts are denied.\n' >&2
  fi
fi

printf 'Preflight: PASS\n'

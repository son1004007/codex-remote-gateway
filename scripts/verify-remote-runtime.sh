#!/usr/bin/env bash
set -euo pipefail

target_label=${1:-target}
key_file=${SSH_KEY_FILE:-$HOME/.ssh/verify_key}
remote_dir=${REMOTE_DIR:-codex-remote-gateway}

fail_local() {
  printf '%s runtime verification failed: %s\n' "$target_label" "$1" >&2
  exit 1
}

[ -n "${SSH_HOST:-}" ] || fail_local missing-ssh-host
[ -n "${SSH_PORT:-}" ] || fail_local missing-ssh-port
[ -n "${SSH_USER:-}" ] || fail_local missing-ssh-user
[ -n "${EXPECTED_SHA:-}" ] || fail_local missing-expected-sha
[ -s "$key_file" ] || fail_local missing-ssh-key
[ -s "$HOME/.ssh/known_hosts" ] || fail_local missing-known-hosts

case "$SSH_PORT" in
  *[!0-9]*|'') fail_local invalid-ssh-port ;;
esac

ssh_error=$(mktemp)
trap 'rm -f "$ssh_error"' EXIT

set +e
result=$(ssh -i "$key_file" \
  -p "$SSH_PORT" \
  -o BatchMode=yes \
  -o ConnectTimeout=20 \
  -o IdentitiesOnly=yes \
  -o StrictHostKeyChecking=yes \
  "$SSH_USER@$SSH_HOST" \
  bash -s -- "$EXPECTED_SHA" "$remote_dir" 2>"$ssh_error" <<'REMOTE'
set -u
expected_sha=$1
remote_dir=$2

fail() {
  printf 'VERIFY_ERROR=%s\n' "$1"
  exit 1
}

case "$remote_dir" in
  /*) ;;
  *) remote_dir="$HOME/$remote_dir" ;;
esac

[ -d "$remote_dir/.git" ] || fail repository
cd "$remote_dir" || fail repository

deployed_sha=$(git rev-parse HEAD 2>/dev/null) || fail repository
[ "$deployed_sha" = "$expected_sha" ] || fail sha-mismatch

[ -f scripts/load-deployment-env.sh ] || fail deployment-env-loader
# shellcheck disable=SC1091
source scripts/load-deployment-env.sh
load_deployment_env "$remote_dir/.env" || fail deployment-env
base_url="http://127.0.0.1:${GATEWAY_PORT:-18080}"

health=$(curl -fsS --max-time 10 "$base_url/actuator/health" 2>/dev/null) || fail health
printf '%s' "$health" | grep -q '"status":"UP"' || fail health

page=$(curl -fsS --max-time 10 "$base_url/" 2>/dev/null) || fail ui
printf '%s' "$page" | grep -Fq 'Codex Remote Gateway' || fail ui

workspaces=$(curl -fsS --max-time 10 "$base_url/api/v1/workspaces" 2>/dev/null) || fail workspaces
printf '%s' "$workspaces" | grep -q '"id":"smoke"' || fail smoke-workspace

sessions=$(curl -fsS --max-time 10 "$base_url/api/v1/sessions" 2>/dev/null) || fail sessions
printf '%s' "$sessions" | grep -q '"workspaceId":"smoke"' || fail smoke-session
printf '%s' "$sessions" | grep -Fq 'gateway-ok' || fail smoke-first-turn
printf '%s' "$sessions" | grep -Fq 'gateway-resume-ok' || fail smoke-second-turn
printf '%s' "$sessions" | grep -q '"providerThreadId":"' || fail smoke-thread

printf 'VERIFIED_SHA=%s\n' "$deployed_sha"
REMOTE
)
ssh_status=$?
set -e

if [ "$ssh_status" -ne 0 ]; then
  marker=$(printf '%s\n' "$result" | sed -n 's/^VERIFY_ERROR=//p' | tail -1)
  if [ -n "$marker" ]; then
    fail_local "remote-$marker"
  fi

  ssh_text=$(cat "$ssh_error")
  case "$ssh_text" in
    *"Permission denied"*) category=ssh-auth ;;
    *"Host key verification failed"*|*"REMOTE HOST IDENTIFICATION HAS CHANGED"*) category=ssh-host-key ;;
    *"Connection timed out"*|*"Operation timed out"*) category=ssh-timeout ;;
    *"Connection refused"*) category=ssh-refused ;;
    *"Could not resolve hostname"*|*"Name or service not known"*|*"Temporary failure in name resolution"*) category=ssh-dns ;;
    *"Bad port"*|*"Invalid port"*) category=invalid-ssh-port ;;
    *) category=ssh-other ;;
  esac
  fail_local "$category"
fi

verified_sha=$(printf '%s\n' "$result" | sed -n 's/^VERIFIED_SHA=//p' | tail -1)
[ "$verified_sha" = "$EXPECTED_SHA" ] || fail_local missing-verified-sha

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "deployed_sha=$verified_sha" >> "$GITHUB_OUTPUT"
fi

printf '%s runtime verification: PASS\n' "$target_label"

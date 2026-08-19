#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
repo_dir=$(cd "$script_dir/.." && pwd)
cd "$repo_dir"
source "$script_dir/load-deployment-env.sh"
load_deployment_env "$repo_dir/.env"

base_url=${GATEWAY_URL:-http://127.0.0.1:${GATEWAY_PORT:-18080}}

health=$(curl -sS "$base_url/actuator/health")
printf '%s\n' "$health" | grep -q '"status":"UP"' || {
  echo "health check failed: $health" >&2
  exit 1
}

ui_status=$(curl -sS -o /dev/null -w '%{http_code}' "$base_url/")
[ "$ui_status" = "200" ] || {
  echo "browser UI check failed with HTTP $ui_status" >&2
  exit 1
}

create_response=$(curl -sS -X POST "$base_url/api/v1/sessions" \
  -H 'Content-Type: application/json' \
  -d '{"workspaceId":"smoke"}')

session_id=$(printf '%s' "$create_response" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
[ -n "$session_id" ] || {
  echo "session creation failed: $create_response" >&2
  exit 1
}

wait_for_turn() {
  local sid=$1
  local label=$2
  local execution response status

  for _ in $(seq 1 300); do
    execution=$(curl -sS "$base_url/api/v1/sessions/$sid/execution")
    status=$(printf '%s' "$execution" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')
    case "$status" in
      SUCCEEDED)
        response=$(curl -sS "$base_url/api/v1/sessions/$sid")
        printf '%s' "$response"
        return 0
        ;;
      FAILED|CANCELLED)
        echo "$label failed: $execution" >&2
        return 1
        ;;
      IDLE|RUNNING|CANCEL_REQUESTED|"")
        sleep 2
        ;;
      *)
        echo "unexpected execution status for $label: $execution" >&2
        return 1
        ;;
    esac
  done

  echo "$label timed out waiting for asynchronous completion" >&2
  return 1
}

curl -fsS -X POST "$base_url/api/v1/sessions/$session_id/messages" \
  -H 'Content-Type: application/json' \
  -d '{"input":"Reply with exactly gateway-ok and do not use tools."}' >/dev/null

submit_response=$(wait_for_turn "$session_id" "first Codex turn")
provider_thread_id=$(printf '%s' "$submit_response" | sed -n 's/.*"providerThreadId":"\([^"]*\)".*/\1/p')
[ -n "$provider_thread_id" ] || {
  echo "Codex thread was not bound: $submit_response" >&2
  exit 1
}
printf '%s' "$submit_response" | grep -q '"actor":"ASSISTANT"' || {
  echo "assistant event missing: $submit_response" >&2
  exit 1
}
printf '%s' "$submit_response" | grep -q 'gateway-ok' || {
  echo "expected smoke response not found: $submit_response" >&2
  exit 1
}

curl -fsS -X POST "$base_url/api/v1/sessions/$session_id/messages" \
  -H 'Content-Type: application/json' \
  -d '{"input":"Reply with exactly gateway-resume-ok and do not use tools."}' >/dev/null

resume_response=$(wait_for_turn "$session_id" "second Codex turn")
printf '%s' "$resume_response" | grep -Fq "\"providerThreadId\":\"$provider_thread_id\"" || {
  echo "provider thread changed during resume: $resume_response" >&2
  exit 1
}
printf '%s' "$resume_response" | grep -q 'gateway-resume-ok' || {
  echo "second Codex turn did not complete: $resume_response" >&2
  exit 1
}

printf 'Smoke test: PASS\nUI: PASS\nSession: %s\nProvider thread: %s\n' "$session_id" "$provider_thread_id"

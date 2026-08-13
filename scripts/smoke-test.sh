#!/usr/bin/env bash
set -euo pipefail

base_url=${GATEWAY_URL:-http://127.0.0.1:${GATEWAY_PORT:-18080}}

health=$(curl -sS "$base_url/actuator/health")
printf '%s\n' "$health" | grep -q '"status":"UP"' || {
  echo "health check failed: $health" >&2
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

submit_response=$(curl -sS -X POST "$base_url/api/v1/sessions/$session_id/messages" \
  -H 'Content-Type: application/json' \
  -d '{"input":"Reply with exactly gateway-ok and do not use tools."}')

printf '%s' "$submit_response" | grep -q '"providerThreadId":"' || {
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

printf 'Smoke test: PASS\nSession: %s\n' "$session_id"

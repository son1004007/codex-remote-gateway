#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
temp_dir=$(mktemp -d)
trap 'rm -rf "$temp_dir"' EXIT

assert_equals() {
  [ "$1" = "$2" ] || {
    printf 'expected %s, got %s\n' "$2" "$1" >&2
    exit 1
  }
}

printf '%s\n' \
  'GATEWAY_PORT=19090' \
  'WORKSPACE_ROOT=./custom-workspaces' \
  'CODEX_HOME_HOST=./custom-codex-home' \
  'VOLUME_SUFFIX=:Z' \
  'IMAGE_TAG="custom-image"' \
  'IGNORED_VALUE=must-not-be-exported' > "$temp_dir/.env"

(
  unset GATEWAY_PORT WORKSPACE_ROOT CODEX_HOME_HOST VOLUME_SUFFIX IMAGE_TAG IGNORED_VALUE
  source "$script_dir/load-deployment-env.sh"
  load_deployment_env "$temp_dir/.env"
  assert_equals "$GATEWAY_PORT" '19090'
  assert_equals "$WORKSPACE_ROOT" './custom-workspaces'
  assert_equals "$CODEX_HOME_HOST" './custom-codex-home'
  assert_equals "$VOLUME_SUFFIX" ':Z'
  assert_equals "$IMAGE_TAG" 'custom-image'
  [ -z "${IGNORED_VALUE+x}" ] || exit 1
)

mkdir "$temp_dir/bin"
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  '[ "$#" -eq 3 ]' \
  '[ "$1" = "--" ]' \
  'printf "%s" "$3" > "$MOCK_SSH_COMMAND"' > "$temp_dir/bin/ssh"
chmod +x "$temp_dir/bin/ssh"

MOCK_SSH_COMMAND="$temp_dir/remote-command" PATH="$temp_dir/bin:$PATH" \
  bash "$script_dir/remote-target.sh" test-target deploy test-gateway

grep -Fq 'git status --porcelain' "$temp_dir/remote-command"
grep -Fq 'git rev-parse --abbrev-ref HEAD' "$temp_dir/remote-command"
grep -Fq 'git config --get-all remote.origin.url' "$temp_dir/remote-command"
grep -Fq 'git merge --ff-only origin/main' "$temp_dir/remote-command"
grep -Fq '[ -f .env ] || cp .env.example .env' "$temp_dir/remote-command"
! grep -Fq 'reset --hard' "$temp_dir/remote-command"
! grep -Fq 'branch --show-current' "$temp_dir/remote-command"
! grep -Fq 'remote get-url' "$temp_dir/remote-command"
! grep -Fq 'config --get remote.origin.url' "$temp_dir/remote-command"

multi_url_repo="$temp_dir/multi-url-repo"
git init -q "$multi_url_repo"
git -C "$multi_url_repo" remote add origin https://example.invalid/expected.git
git -C "$multi_url_repo" config --add remote.origin.url https://example.invalid/unexpected.git
origin_urls=$(git -C "$multi_url_repo" config --get-all remote.origin.url || true)
if [ "$origin_urls" = 'https://example.invalid/expected.git' ]; then
  echo 'multiple origin URLs unexpectedly passed the exact-match guard' >&2
  exit 1
fi

if MOCK_SSH_COMMAND="$temp_dir/unused" PATH="$temp_dir/bin:$PATH" \
  bash "$script_dir/remote-target.sh" test-target deploy ../unsafe >/dev/null 2>&1; then
  echo 'unsafe remote directory unexpectedly accepted' >&2
  exit 1
fi

if MOCK_SSH_COMMAND="$temp_dir/unused" PATH="$temp_dir/bin:$PATH" \
  bash "$script_dir/remote-target.sh" -unsafe deploy test-gateway >/dev/null 2>&1; then
  echo 'unsafe ssh alias unexpectedly accepted' >&2
  exit 1
fi

login_repo="$temp_dir/login-repo"
mkdir -p "$login_repo"
cp -R "$script_dir" "$login_repo/scripts"
cp "$script_dir/../.env.example" "$login_repo/.env.example"
cp "$temp_dir/.env" "$login_repo/.env"
mkdir "$login_repo/bin"
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'printf "%s\\n" "$*" >> "$MOCK_DOCKER_LOG"' \
  'case "$1 ${2:-}" in' \
  '  "compose version") exit 0 ;;' \
  '  "compose build") exit 0 ;;' \
  '  "compose run")' \
  '    if [[ "$*" == *"--entrypoint /bin/sh gateway -c"* ]]; then exit 0; fi' \
  '    if [[ "$*" == *"--entrypoint codex gateway login status"* ]]; then' \
  '      if [ -f "$MOCK_DOCKER_STATE" ]; then read -r count < "$MOCK_DOCKER_STATE"; else count=0; fi' \
  '      IFS="," read -r -a statuses <<< "$MOCK_LOGIN_STATUS_SEQUENCE"' \
  '      last_index=$((${#statuses[@]} - 1)); [ "$count" -le "$last_index" ] || count=$last_index' \
  '      status=${statuses[$count]}' \
  '      printf "%s" "$((count + 1))" > "$MOCK_DOCKER_STATE"' \
  '      exit "$status"' \
  '    fi' \
  '    if [[ "$*" == *"--entrypoint codex gateway login --device-auth"* ]]; then exit 0; fi' \
  '    ;;' \
  'esac' \
  'exit 64' > "$login_repo/bin/docker"
chmod +x "$login_repo/bin/docker"

run_login_case() {
  local statuses=$1
  local name=$2
  local log="$temp_dir/$name.log"
  local state="$temp_dir/$name.state"

  MOCK_LOGIN_STATUS_SEQUENCE="$statuses" \
    MOCK_DOCKER_LOG="$log" \
    MOCK_DOCKER_STATE="$state" \
    PATH="$login_repo/bin:$PATH" \
    bash "$login_repo/scripts/codex-login.sh" >/dev/null
  printf '%s' "$log"
}

existing_login_log=$(run_login_case '0' existing-login)
[ "$(grep -Fc -- '--entrypoint codex gateway login status' "$existing_login_log")" -eq 1 ]
! grep -Fq -- 'login --device-auth' "$existing_login_log"

new_login_log=$(run_login_case '1,0' new-login)
[ "$(grep -Fc -- '--entrypoint codex gateway login status' "$new_login_log")" -eq 2 ]
[ "$(grep -Fc -- '--entrypoint codex gateway login --device-auth' "$new_login_log")" -eq 1 ]

printf 'Deployment script tests: PASS\n'

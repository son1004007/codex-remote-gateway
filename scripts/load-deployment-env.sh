#!/usr/bin/env bash

# Load only the documented deployment settings without evaluating .env as shell code.
# Explicit process environment values take precedence, matching Docker Compose.
load_deployment_env() {
  local env_file=${1:-.env}
  local line key value

  [ -f "$env_file" ] || return 0

  while IFS= read -r line || [ -n "$line" ]; do
    line=${line%$'\r'}
    case "$line" in
      ''|'#'*) continue ;;
      *=*) ;;
      *)
        printf 'WARN: ignoring malformed deployment environment line in %s\n' "$env_file" >&2
        continue
        ;;
    esac

    key=${line%%=*}
    value=${line#*=}
    case "$key" in
      GATEWAY_BIND_ADDRESS|GATEWAY_PORT|GATEWAY_URL|CODEX_VERSION|IMAGE_TAG|WORKSPACE_ROOT|CODEX_HOME_HOST|VOLUME_SUFFIX|GATEWAY_CODEX_APPROVAL_POLICY|GATEWAY_CODEX_SANDBOX|GATEWAY_CODEX_MODEL|GATEWAY_CODEX_TURN_TIMEOUT|GATEWAY_ANTIGRAVITY_ENABLED|GATEWAY_ANTIGRAVITY_COMMAND|GATEWAY_ANTIGRAVITY_MODEL|GATEWAY_ANTIGRAVITY_TURN_TIMEOUT) ;;
      *) continue ;;
    esac

    if [[ -v $key ]]; then
      continue
    fi

    case "$value" in
      \"*\") value=${value#\"}; value=${value%\"} ;;
      \'*\') value=${value#\'}; value=${value%\'} ;;
    esac
    export "$key=$value"
  done < "$env_file"
}

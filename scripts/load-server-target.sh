#!/usr/bin/env bash
set -euo pipefail

load_server_target() {
  local target=${1:-}
  local file=${2:-config/server-targets.properties}

  case "$target" in
    office|idc) ;;
    *) echo "Unsupported server target: $target" >&2; return 2 ;;
  esac

  [ -f "$file" ] || {
    echo "Server target config not found: $file" >&2
    return 2
  }

  local name='' host='' port='' user='' runtime=''
  local key value
  while IFS='=' read -r key value; do
    case "$key" in
      ''|'#'*) continue ;;
      "$target.name") [ -z "$name" ] || return 2; name=$value ;;
      "$target.host") [ -z "$host" ] || return 2; host=$value ;;
      "$target.port") [ -z "$port" ] || return 2; port=$value ;;
      "$target.user") [ -z "$user" ] || return 2; user=$value ;;
      "$target.runtime") [ -z "$runtime" ] || return 2; runtime=$value ;;
    esac
  done < "$file"

  [ -n "$name" ] && [ -n "$host" ] && [ -n "$port" ] && [ -n "$user" ] && [ -n "$runtime" ] || {
    echo "Incomplete server target config: $target" >&2
    return 2
  }

  case "$host" in
    *[!A-Za-z0-9._:-]*) echo "Invalid target host for $target" >&2; return 2 ;;
  esac
  case "$port" in
    *[!0-9]*|'') echo "Invalid target port for $target" >&2; return 2 ;;
  esac
  case "$user" in
    *[!A-Za-z0-9._-]*|'') echo "Invalid target user for $target" >&2; return 2 ;;
  esac
  case "$runtime" in
    host-local|compose) ;;
    *) echo "Invalid target runtime for $target" >&2; return 2 ;;
  esac

  export TARGET_NAME="$name"
  export SSH_HOST="$host"
  export SSH_PORT="$port"
  export SSH_USER="$user"
  export TARGET_RUNTIME="$runtime"
}

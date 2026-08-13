#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  bash scripts/remote-target.sh <ssh-alias> sync       [remote-dir]
  bash scripts/remote-target.sh <ssh-alias> preflight  [remote-dir]
  bash scripts/remote-target.sh <ssh-alias> deploy [remote-dir]
  bash scripts/remote-target.sh <ssh-alias> login  [remote-dir]
  bash scripts/remote-target.sh <ssh-alias> smoke  [remote-dir]

The SSH alias must be defined outside this public repository, for example in ~/.ssh/config.
The default remote directory is codex-remote-gateway under the remote login user's home/current SSH directory.
sync creates a clone or performs a clean fast-forward update only; it never resets a remote working tree.
EOF
}

[ $# -ge 2 ] || { usage; exit 1; }
target=$1
action=$2
remote_dir=${3:-codex-remote-gateway}
repo_url=${GATEWAY_REPO_URL:-https://github.com/son1004007/codex-remote-gateway.git}

if [[ ! "$target" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]]; then
  echo "ssh-alias must be a safe OpenSSH host alias" >&2
  exit 1
fi

if [[ ! "$remote_dir" =~ ^[A-Za-z0-9][A-Za-z0-9._/-]*$ ]] || [ "$remote_dir" = "." ] || [[ "$remote_dir" = /* ]] || [[ "$remote_dir" == *..* ]]; then
  echo "remote-dir must be a safe, relative path without '..'" >&2
  exit 1
fi

if [[ ! "$repo_url" =~ ^(https://|ssh://|git@)[A-Za-z0-9._:/@+-]+$ ]]; then
  echo "GATEWAY_REPO_URL contains unsupported characters" >&2
  exit 1
fi

sync_command=$(cat <<EOF
set -euo pipefail
remote_dir='$remote_dir'
repo_url='$repo_url'

if [ -e "\$remote_dir" ] || [ -L "\$remote_dir" ]; then
  [ -d "\$remote_dir" ] && [ ! -L "\$remote_dir" ] && [ -d "\$remote_dir/.git" ] || {
    echo "ERROR: remote deployment path exists but is not a regular Git working tree: \$remote_dir" >&2
    exit 1
  }
  cd "\$remote_dir"
  [ -z "\$(git status --porcelain)" ] || {
    echo "ERROR: remote gateway working tree is dirty; refusing deployment" >&2
    exit 1
  }
  [ "\$(git rev-parse --abbrev-ref HEAD)" = "main" ] || {
    echo "ERROR: remote gateway is not on main; refusing deployment" >&2
    exit 1
  }
  origin_urls=\$(git config --get-all remote.origin.url || true)
  effective_fetch_urls=\$(git remote -v | awk '\$1 == "origin" && \$3 == "(fetch)" {print \$2}')
  [ "\$origin_urls" = "\$repo_url" ] && [ "\$effective_fetch_urls" = "\$repo_url" ] || {
    echo "ERROR: remote origin does not match GATEWAY_REPO_URL; refusing deployment" >&2
    exit 1
  }
  git fetch origin
  git merge-base --is-ancestor HEAD origin/main || {
    echo "ERROR: remote gateway has diverged from origin/main; refusing deployment" >&2
    exit 1
  }
  git merge --ff-only origin/main
else
  git clone --branch main --single-branch "\$repo_url" -- "\$remote_dir"
  cd "\$remote_dir"
fi

[ -f .env ] || cp .env.example .env
EOF
)

case "$action" in
  sync)
    ssh -- "$target" "$sync_command"
    ;;
  preflight)
    ssh -- "$target" "set -euo pipefail; cd '$remote_dir'; bash scripts/preflight.sh"
    ;;
  deploy)
    ssh -- "$target" "$sync_command
bash scripts/deploy-local.sh"
    ;;
  login)
    ssh -t -- "$target" "cd '$remote_dir' && bash scripts/codex-login.sh"
    ;;
  smoke)
    ssh -- "$target" "cd '$remote_dir' && bash scripts/smoke-test.sh"
    ;;
  *)
    usage
    exit 1
    ;;
esac

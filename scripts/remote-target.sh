#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  bash scripts/remote-target.sh <ssh-alias> deploy [remote-dir]
  bash scripts/remote-target.sh <ssh-alias> login  [remote-dir]
  bash scripts/remote-target.sh <ssh-alias> smoke  [remote-dir]

The SSH alias must be defined outside this public repository, for example in ~/.ssh/config.
The default remote directory is codex-remote-gateway under the remote login user's home/current SSH directory.
EOF
}

[ $# -ge 2 ] || { usage; exit 1; }
target=$1
action=$2
remote_dir=${3:-codex-remote-gateway}
repo_url=${GATEWAY_REPO_URL:-https://github.com/son1004007/codex-remote-gateway.git}

case "$action" in
  deploy)
    ssh "$target" "set -e; if [ -d '$remote_dir/.git' ]; then cd '$remote_dir' && git fetch origin main && git reset --hard origin/main; else git clone '$repo_url' '$remote_dir'; fi; cd '$remote_dir'; bash scripts/deploy-local.sh"
    ;;
  login)
    ssh -t "$target" "cd '$remote_dir' && bash scripts/codex-login.sh"
    ;;
  smoke)
    ssh "$target" "cd '$remote_dir' && bash scripts/smoke-test.sh"
    ;;
  *)
    usage
    exit 1
    ;;
esac

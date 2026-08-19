#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
temp_dir=$(mktemp -d)
trap 'rm -rf "$temp_dir"' EXIT

assert_fails_with() {
  local expected=$1
  shift
  local output
  if output=$("$@" 2>&1); then
    echo "expected command to fail with $expected" >&2
    exit 1
  fi
  printf '%s\n' "$output" | grep -Fq "$expected" || {
    printf 'expected failure category %s, got:\n%s\n' "$expected" "$output" >&2
    exit 1
  }
}

# Required connection fields are aggregated without attempting SSH or printing values.
assert_fails_with 'missing-config:ssh-host,ssh-port,ssh-user,expected-sha,ssh-key,known-hosts' \
  env -u SSH_HOST -u SSH_PORT -u SSH_USER -u EXPECTED_SHA \
      HOME="$temp_dir/empty-home" \
      SSH_KEY_FILE="$temp_dir/missing-key" \
  bash "$script_dir/verify-remote-runtime.sh" test-target

# Invalid ports are rejected locally after required secret-file preconditions pass.
mkdir -p "$temp_dir/home/.ssh"
printf 'dummy-key\n' > "$temp_dir/key"
printf 'dummy-known-host\n' > "$temp_dir/home/.ssh/known_hosts"
assert_fails_with 'invalid-ssh-port' \
  env HOME="$temp_dir/home" \
      SSH_KEY_FILE="$temp_dir/key" \
      SSH_HOST='example.invalid' \
      SSH_PORT='not-a-port' \
      SSH_USER='test-user' \
      EXPECTED_SHA='0000000000000000000000000000000000000000' \
  bash "$script_dir/verify-remote-runtime.sh" test-target

# A mocked SSH authentication failure must be reduced to a generic category; raw endpoint text must not leak.
mkdir -p "$temp_dir/bin"
cat > "$temp_dir/bin/ssh" <<'EOF'
#!/usr/bin/env bash
printf 'test-user@private.example.invalid: Permission denied (publickey).\n' >&2
exit 255
EOF
chmod +x "$temp_dir/bin/ssh"
output=$(env HOME="$temp_dir/home" \
    PATH="$temp_dir/bin:$PATH" \
    SSH_KEY_FILE="$temp_dir/key" \
    SSH_HOST='private.example.invalid' \
    SSH_PORT='2222' \
    SSH_USER='test-user' \
    EXPECTED_SHA='0000000000000000000000000000000000000000' \
    bash "$script_dir/verify-remote-runtime.sh" test-target 2>&1 || true)
printf '%s\n' "$output" | grep -Fq 'ssh-auth'
! printf '%s\n' "$output" | grep -Fq 'private.example.invalid'

printf 'Runtime verifier tests: PASS\n'

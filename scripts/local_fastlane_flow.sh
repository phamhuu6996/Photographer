#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

log() {
  printf "\n%s\n" "==> $1"
}

export PLAY_JSON_KEY_PATH="${PLAY_JSON_KEY_PATH:-/path/to/your/service_account.json}"
if [[ ! -f "$PLAY_JSON_KEY_PATH" ]]; then
  log "WARNING: PLAY_JSON_KEY_PATH ($PLAY_JSON_KEY_PATH) does not exist. Deploy lanes will fail."
fi

log "Installing/updating Ruby gems"
bundle install

log "Running unit tests"
bundle exec fastlane android test

log "Running lint"
bundle exec fastlane android lint

log "Building release App Bundle"
bundle exec fastlane android build_bundle

cat <<'EOF'

✅ Local Fastlane flow completed.
- AAB: app/build/outputs/bundle/release/app-release.aab

If you need to deploy:
  bundle exec fastlane android beta   # Internal testing
  bundle exec fastlane android release

EOF


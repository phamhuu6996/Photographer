#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

log() {
  printf "\n%s\n" "==> $1"
}

export PLAY_JSON_KEY_PATH="${PLAY_JSON_KEY_PATH:-/Users/phamhuu/Documents/out_project/Photographer/beatycam-819d0-e39f82e08c12.json}"
if [[ ! -f "$PLAY_JSON_KEY_PATH" ]]; then
  log "WARNING: PLAY_JSON_KEY_PATH ($PLAY_JSON_KEY_PATH) does not exist. Deploy lanes will fail."
fi

log "Installing/updating Ruby gems"
bundle install

# log "Running unit tests"
# bundle exec fastlane android test

# log "Running lint"
# bundle exec fastlane android lint

log "Bumping version (code + name)"
bundle exec fastlane android bump_version

log "Building release App Bundle"
bundle exec fastlane android build_bundle

log "Uploading to Play Store (Internal testing track)"
bundle exec fastlane android beta

cat <<'EOF'

✅ Local Fastlane flow completed.
- AAB: app/build/outputs/bundle/release/app-release.aab
- Internal testing + production uploads have been triggered.

EOF


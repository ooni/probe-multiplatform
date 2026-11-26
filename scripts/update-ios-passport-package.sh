#!/usr/bin/env bash
# Refresh the local Swift package at iosApp/Packages/Passport from an
# ooniprobe-rs GitHub release (default: the `rolling` tag).
#
# The release ships Package.swift (with the xcframework URL and checksum)
# and ooniprobe.swift (the UniFFI bindings). Both must come from the same
# release, so always update them together.
#
# Usage: scripts/update-ios-passport-package.sh [release-tag]
set -euo pipefail

TAG="${1:-rolling}"
BASE_URL="https://github.com/ooni/ooniprobe-rs/releases/download/${TAG}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PKG_DIR="${ROOT_DIR}/iosApp/Packages/Passport"

mkdir -p "${PKG_DIR}/Sources/Passport"
curl -fsSL "${BASE_URL}/Package.swift" -o "${PKG_DIR}/Package.swift"
curl -fsSL "${BASE_URL}/ooniprobe.swift" -o "${PKG_DIR}/Sources/Passport/ooniprobe.swift"

echo "Updated ${PKG_DIR} from ooniprobe-rs release '${TAG}'."
echo "In Xcode: File > Packages > Reset Package Caches if the checksum changed."

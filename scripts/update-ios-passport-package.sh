#!/usr/bin/env bash
# Refresh the local Swift package at iosApp/Packages/Passport from an ooni/ooniprobe-rs
# GitHub release.
#
# Each release publishes two files that must come from the same build:
#   - Package.swift, with the passport.xcframework URL and its SHA-256 checksum;
#   - ooniprobe.swift, the UniFFI Swift bindings (copied to Sources/Passport/).
# A release without both files is not usable, and the script fails.
#
# Usage: scripts/update-ios-passport-package.sh [tag | release]
#
#   tag      (default: rolling) the release tag, e.g. v0.1.9. The `rolling` release is
#            overwritten on every ooniprobe-rs build, so its checksum goes stale; run the
#            script again when that happens.
#   release  the tag for `passport` in gradle/libs.versions.toml (v<version>), the version
#            Android and Desktop use.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PKG_DIR="${ROOT_DIR}/iosApp/Packages/Passport"
TAG="${1:-rolling}"
if [ "${TAG}" = "release" ]; then
    VERSION="$(sed -n 's/^passport = "\(.*\)"$/\1/p' "${ROOT_DIR}/gradle/libs.versions.toml")"
    if [ -z "${VERSION}" ]; then
        echo "Could not read the passport version from gradle/libs.versions.toml." >&2
        exit 1
    fi
    TAG="v${VERSION}"
fi
BASE_URL="https://github.com/ooni/ooniprobe-rs/releases/download/${TAG}"

work="$(mktemp -d)"
trap 'rm -rf "${work}"' EXIT

for file in Package.swift ooniprobe.swift; do
    if ! curl -fsSL -o "${work}/${file}" "${BASE_URL}/${file}"; then
        echo "ooniprobe-rs ${TAG} has no ${file} (${BASE_URL}/${file})." >&2
        echo "Every release used by the iOS app must publish Package.swift and ooniprobe.swift." >&2
        exit 1
    fi
done
if ! grep -q 'import PackageDescription' "${work}/Package.swift"; then
    echo "${BASE_URL}/Package.swift does not look like a Package.swift" >&2
    exit 1
fi

mkdir -p "${PKG_DIR}/Sources/Passport"
mv "${work}/Package.swift" "${PKG_DIR}/Package.swift"
mv "${work}/ooniprobe.swift" "${PKG_DIR}/Sources/Passport/ooniprobe.swift"

echo "Updated ${PKG_DIR} from ooniprobe-rs ${TAG}."
echo "In Xcode: File > Packages > Reset Package Caches, then build."

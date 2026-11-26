#!/usr/bin/env bash
# Refresh iosApp/Packages/Oonimkall/Package.swift from an ooni/probe-cli GitHub release.
#
# probe-cli publishes a Package.swift with each release. It declares oonimkall and the
# native libraries it links against (Tor, OpenSSL, libevent, zlib) as remote binary
# targets, with their SHA-256 checksums. composeApp declares this local package in
# `kotlin { swiftPMDependencies { ... } }`, and Xcode links it through
# iosApp/KotlinMultiplatformLinkedPackage.
#
# The `rolling` release is overwritten on every probe-cli master build, so the pinned
# checksums go stale when it changes. Run this script again to pick up the new build.
#
# Prefer a release tag once probe-cli publishes Package.swift with tagged releases: tag
# assets never change. Up to v3.31.1, tagged releases ship no Package.swift, and their
# Tor/OpenSSL/libevent/zlib xcframeworks contain macOS-style frameworks that Xcode can't
# code-sign, so `rolling` is the only usable source for now.
#
# Usage: scripts/update-ios-oonimkall-package.sh [release-tag]   (default: rolling)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TAG="${1:-rolling}"
URL="https://github.com/ooni/probe-cli/releases/download/${TAG}/Package.swift"
PKG_DIR="${ROOT_DIR}/iosApp/Packages/Oonimkall"

mkdir -p "${PKG_DIR}"
tmp="$(mktemp)"
trap 'rm -f "${tmp}"' EXIT

if ! curl -fsSL -o "${tmp}" "${URL}"; then
    echo "probe-cli ${TAG} has no Package.swift (${URL}). Use a release that publishes one, or rolling." >&2
    exit 1
fi
if ! grep -q 'import PackageDescription' "${tmp}"; then
    echo "${URL} does not look like a Package.swift" >&2
    exit 1
fi
mv "${tmp}" "${PKG_DIR}/Package.swift"
trap - EXIT

echo "Updated ${PKG_DIR}/Package.swift from probe-cli ${TAG}."
echo "In Xcode: File > Packages > Reset Package Caches, then build."

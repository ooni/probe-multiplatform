#!/bin/bash

# Apply entitlements to OONI Probe.app after build
# This script re-signs the app with the correct entitlements

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
APP_PATH="$PROJECT_DIR/composeApp/build/compose/binaries/main/app/OONI Probe.app"
ENTITLEMENTS="$PROJECT_DIR/composeApp/OONIProbe.entitlements"

if [ ! -d "$APP_PATH" ]; then
    echo "❌ App not found at: $APP_PATH"
    exit 1
fi

if [ ! -f "$ENTITLEMENTS" ]; then
    echo "❌ Entitlements file not found at: $ENTITLEMENTS"
    exit 1
fi

echo "🔏 Applying entitlements to OONI Probe.app..."
echo "   App: $APP_PATH"
echo "   Entitlements: $ENTITLEMENTS"
echo ""

# IMPORTANT: Do NOT use --deep flag!
# Sparkle's XPC services must remain UNSANDBOXED to install updates.
# Only sign the main app bundle with sandboxed entitlements.

echo "Signing main app bundle (without --deep to preserve Sparkle XPC services)..."
codesign --force --sign - --entitlements "$ENTITLEMENTS" "$APP_PATH"

# Verify the signature
echo ""
echo "✅ Verifying signature..."
codesign -vvv "$APP_PATH"

# Show applied entitlements
echo ""
echo "📋 Applied entitlements:"
codesign -d --entitlements :- "$APP_PATH" 2>&1 | grep -A 5 "mach-register\|downloads" || echo "   (entitlements applied)"

echo ""
echo "✅ Entitlements applied successfully!"
echo ""
echo "To verify all entitlements:"
echo "   codesign -d --entitlements - \"$APP_PATH\""

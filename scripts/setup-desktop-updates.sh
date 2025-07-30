#!/bin/bash

# Desktop Update System Setup Script
# This script downloads and sets up Sparkle and WinSparkle dependencies

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DESKTOP_MAIN_DIR="$PROJECT_ROOT/composeApp/src/desktopMain"

echo "🚀 Setting up Desktop Update System for OONI Probe"
echo "Project root: $PROJECT_ROOT"

# Detect platform
PLATFORM=""
if [[ "$OSTYPE" == "darwin"* ]]; then
    PLATFORM="macos"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    PLATFORM="linux"
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    PLATFORM="windows"
else
    echo "❌ Unsupported platform: $OSTYPE"
    exit 1
fi

echo "📱 Detected platform: $PLATFORM"

# Create directories
mkdir -p "$DESKTOP_MAIN_DIR/resources/macos"
mkdir -p "$DESKTOP_MAIN_DIR/resources/windows"
mkdir -p "$DESKTOP_MAIN_DIR/build"

# Download Sparkle for macOS
# if [[ "$PLATFORM" == "macos" ]]; then
echo "📦 Setting up Sparkle for macOS..."

SPARKLE_URL="https://github.com/sparkle-project/Sparkle/releases/latest/download/Sparkle-for-Swift-Package-Manager.zip"
SPARKLE_ZIP="$DESKTOP_MAIN_DIR/sparkle.zip"

if [[ ! -d "$DESKTOP_MAIN_DIR/resources/macos/Sparkle.framework" ]]; then
    echo "⬇️  Downloading Sparkle framework..."
    curl -L "$SPARKLE_URL" -o "$SPARKLE_ZIP"

    echo "📂 Extracting Sparkle framework to resources/macos..."
    cd "$DESKTOP_MAIN_DIR"
    unzip -q "$SPARKLE_ZIP"

    # Copy the framework from XCFramework to resources/macos
    cp -R Sparkle.xcframework/macos-arm64_x86_64/Sparkle.framework resources/macos/

    # Cleanup
    rm -rf Sparkle.xcframework bin CHANGELOG INSTALL LICENSE SampleAppcast.xml
    rm "$SPARKLE_ZIP"

    echo "✅ Sparkle framework installed"
else
    echo "✅ Sparkle framework already exists"
fi

# Check if EdDSA keys exist
SPARKLE_TOOLS="$DESKTOP_MAIN_DIR/resources/macos/Sparkle.framework/Versions/B/Resources"
if [[ -f "$SPARKLE_TOOLS/generate_keys" ]]; then
    echo "🔑 Sparkle tools available at: $SPARKLE_TOOLS"
    echo "   Run the following to generate EdDSA keys:"
    echo "   cd '$SPARKLE_TOOLS' && ./generate_keys"
fi
# fi

# Build native libraries
echo "🔨 Building native libraries..."
cd "$DESKTOP_MAIN_DIR"

if command -v make >/dev/null 2>&1; then
    make clean
    make all
    echo "✅ Native libraries built successfully"
else
    echo "⚠️  Make not found. Please install build tools and run 'make all' in $DESKTOP_MAIN_DIR"
fi

# Download WinSparkle for Windows (after build to avoid being cleaned)
# if [[ "$PLATFORM" == "windows" ]]; then
echo "📦 Setting up WinSparkle for Windows..."

WINSPARKLE_URL="https://github.com/vslavik/winsparkle/releases/download/v0.9.1/WinSparkle-0.9.1.zip"
WINSPARKLE_ZIP="$DESKTOP_MAIN_DIR/winsparkle.zip"

# Ensure windows resources directory exists
mkdir -p "$DESKTOP_MAIN_DIR/resources/windows"

if [[ ! -f "$DESKTOP_MAIN_DIR/resources/windows/WinSparkle.dll" ]]; then
    echo "⬇️  Downloading WinSparkle..."
    curl -L "$WINSPARKLE_URL" -o "$WINSPARKLE_ZIP"

    echo "📂 Extracting WinSparkle..."
    cd "$DESKTOP_MAIN_DIR"
    # Remove any existing WinSparkle directory first
    rm -rf WinSparkle-*
    unzip -o -q "$WINSPARKLE_ZIP"

    # Copy DLL to resources/windows directory
    cp WinSparkle-*/Release/WinSparkle.dll resources/windows/
    cp WinSparkle-*/Release/WinSparkle.lib resources/windows/
    cp WinSparkle-*/x64/Release/WinSparkle.dll resources/windows/WinSparkle-x64.dll
    cp WinSparkle-*/x64/Release/WinSparkle.lib resources/windows/WinSparkle-x64.lib
    cp WinSparkle-*/ARM64/Release/WinSparkle.dll resources/windows/WinSparkle-ARM64.dll
    cp WinSparkle-*/ARM64/Release/WinSparkle.lib resources/windows/WinSparkle-ARM64.lib

    # Copy headers
    mkdir -p resources/windows/include
    cp WinSparkle-*/include/* resources/windows/include/

    # Cleanup
    rm -rf WinSparkle-*
    rm "$WINSPARKLE_ZIP"

    echo "✅ WinSparkle installed"
else
    echo "✅ WinSparkle already exists"
fi
# fi

# Validate setup
echo "🔍 Validating setup..."

# if [[ "$PLATFORM" == "macos" ]]; then
if [[ -f "$DESKTOP_MAIN_DIR/resources/macos/libnetworktypefinder.dylib" ]]; then
    echo "✅ NetworkTypeFinder library built"
else
    echo "❌ NetworkTypeFinder library missing"
fi

if [[ -f "$DESKTOP_MAIN_DIR/resources/macos/libupdatebridge.dylib" ]]; then
    echo "✅ UpdateBridge library built"
else
    echo "❌ UpdateBridge library missing"
fi

if [[ -d "$DESKTOP_MAIN_DIR/resources/macos/Sparkle.framework" ]]; then
    echo "✅ Sparkle framework available"
else
    echo "❌ Sparkle framework missing"
fi
# fi

# if [[ "$PLATFORM" == "windows" ]]; then
if [[ -f "$DESKTOP_MAIN_DIR/resources/windows/networktypefinder.dll" ]]; then
    echo "✅ NetworkTypeFinder library built"
else
    echo "❌ NetworkTypeFinder library missing"
fi

if [[ -f "$DESKTOP_MAIN_DIR/resources/windows/updatebridge.dll" ]]; then
    echo "✅ UpdateBridge library built"
else
    echo "❌ UpdateBridge library missing"
fi

if [[ -f "$DESKTOP_MAIN_DIR/resources/windows/WinSparkle.dll" ]]; then
    echo "✅ WinSparkle DLL available"
else
    echo "❌ WinSparkle DLL missing"
fi

if [[ -f "$DESKTOP_MAIN_DIR/resources/windows/WinSparkle-x64.dll" ]]; then
    echo "✅ WinSparkle x64 DLL available"
else
    echo "❌ WinSparkle x64 DLL missing"
fi

if [[ -f "$DESKTOP_MAIN_DIR/resources/windows/WinSparkle-ARM64.dll" ]]; then
    echo "✅ WinSparkle ARM64 DLL available"
else
    echo "❌ WinSparkle ARM64 DLL missing"
fi

if [[ -d "$DESKTOP_MAIN_DIR/resources/windows/include" ]]; then
    echo "✅ WinSparkle headers available"
else
    echo "❌ WinSparkle headers missing"
fi
# fi

echo ""
echo "🎉 Desktop Update System setup complete!"
echo ""
echo "Next steps:"
echo "1. For macOS: Generate EdDSA keys using Sparkle tools"
echo "2. Update conveyor.conf with your EdDSA public key"
echo "3. Set up your appcast feed server"
echo "4. Build the application: ./gradlew :composeApp:packageDistributionForCurrentOS"

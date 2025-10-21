#!/bin/bash
set -e

# Script to create AppImage from OONI Probe distributable
# Usage: ./scripts/create-appimage.sh [version]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Colors for output (only used when stdout is a TTY)
_IS_TTY=0
if [ -t 1 ]; then
    _IS_TTY=1
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Gradle-style logging helpers
# Usage: log.lifecycle "message"; log.info "message"; log.warn "message"; log.error "message"; log.success "message"
log_prefix() {
    local level="$1"
    printf "%s" "[${level}]"
}

log_print() {
    local level="$1"; shift
    local color="$1"; shift
    local msg="$*"
    if [ "${_IS_TTY}" -eq 1 ]; then
        printf "%s %b%s%b\n" "$(log_prefix "$level")" "${color}" "${msg}" "${NC}"
    else
        printf "%s %s\n" "$(log_prefix "$level")" "${msg}"
    fi
}

log.lifecycle() { log_print LIFECYCLE "${GREEN}" "$*"; }
log.info()      { log_print INFO      ""         "$*"; }
log.warn()      { log_print WARN      "${YELLOW}" "$*"; }
log.error()     { log_print ERROR     "${RED}"    "$*"; }
log.success()   { log_print SUCCESS   "${GREEN}" "$*"; }
# Highlight helper for important single-line values (paths, commands)
log.highlight() {
    local msg="$*"
    if [ "${_IS_TTY}" -eq 1 ]; then
        printf "  %b%s%b\n" "${YELLOW}" "${msg}" "${NC}"
    else
        printf "  %s\n" "${msg}"
    fi
}

# Configuration
APP_NAME="OONI Probe"
APP_ID="org.ooni.probe"
DESKTOP_FILE_NAME="ooniprobe"
BUILD_DIR="${PROJECT_ROOT}/composeApp/build/compose/binaries/main/app"
DIST_DIR="${BUILD_DIR}/${APP_NAME}"
APPDIR_NAME="OONIProbe.AppDir"
APPIMAGE_TOOL_URL="https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage"

# Get version from gradle.properties or use parameter
if [ -n "$1" ]; then
    VERSION="$1"
else
    VERSION_FILE="${PROJECT_ROOT}/composeApp/build.gradle.kts"

    VERSION=""

    # Try to extract versionName from composeApp/build.gradle.kts (supports single/double quotes)
    if [ -f "$VERSION_FILE" ]; then
        # Use grep -Po to extract the versionName value (handles single or double quotes)
        VERSION=$(grep -Po "versionName\s*=\s*['\"]\K[^'\"]+" "$VERSION_FILE" | head -n1 || true)
    fi

    # Final fallback
    if [ -z "$VERSION" ]; then
        VERSION="1.0.0"
    fi
fi

log.lifecycle "OONI Probe AppImage Creator"
log.info "Version: ${VERSION}"
log.info "Project Root: ${PROJECT_ROOT}"


# Check if distributable exists
if [ ! -d "${DIST_DIR}" ]; then
    log.warn "Distributable not found. Building..."
    cd "${PROJECT_ROOT}"
    ./gradlew createDistributable

    if [ ! -d "${DIST_DIR}" ]; then
        log.error "Error: Failed to create distributable at ${DIST_DIR}"
        exit 1
    fi
fi

log.success "✓ Distributable found"

# Create workspace
WORKSPACE="${PROJECT_ROOT}/composeApp/build/compose/binaries/main/appimage-workspace"
mkdir -p "${WORKSPACE}"
cd "${WORKSPACE}"

log.lifecycle "Creating AppDir structure..."

# Clean previous AppDir if exists
rm -rf "${APPDIR_NAME}"
mkdir -p "${APPDIR_NAME}/usr"

# Copy application files
log.info "Copying application files..."
cp -r "${DIST_DIR}/bin" "${APPDIR_NAME}/usr/"
cp -r "${DIST_DIR}/lib" "${APPDIR_NAME}/usr/"

# Create AppRun script
log.info "Creating AppRun script..."
cat > "${APPDIR_NAME}/AppRun" << 'EOF'
#!/bin/bash
# AppRun script for OONI Probe

# Get the directory where the AppImage is mounted
HERE="$(dirname "$(readlink -f "${0}")")"

# Set up library paths
export LD_LIBRARY_PATH="${HERE}/usr/lib:${HERE}/usr/lib/runtime/lib:${LD_LIBRARY_PATH}"
export PATH="${HERE}/usr/bin:${PATH}"

# Set up Java-related paths
export JAVA_HOME="${HERE}/usr/lib/runtime"
export PATH="${JAVA_HOME}/bin:${PATH}"

# Launch OONI Probe
exec "${HERE}/usr/bin/OONI Probe" "$@"
EOF

# Create desktop entry
log.info "Creating desktop entry..."
cat > "${APPDIR_NAME}/${DESKTOP_FILE_NAME}.desktop" << EOF
[Desktop Entry]
Type=Application
Name=OONI Probe
GenericName=Network Measurement Tool
Comment=Measure internet censorship and network interference
Exec=OONI Probe %u
Icon=${DESKTOP_FILE_NAME}
Categories=Network;Utility;
Terminal=false
StartupWMClass=OONI Probe
Keywords=censorship;network;measurement;ooni;
MimeType=x-scheme-handler/ooni;
EOF

# Copy icon
log.info "Copying application icon..."
if [ -f "${DIST_DIR}/lib/${APP_NAME}.png" ]; then
    cp "${DIST_DIR}/lib/${APP_NAME}.png" "${APPDIR_NAME}/${DESKTOP_FILE_NAME}.png"
elif [ -f "${PROJECT_ROOT}/icons/app.png" ]; then
    cp "${PROJECT_ROOT}/icons/app.png" "${APPDIR_NAME}/${DESKTOP_FILE_NAME}.png"
else
    log.warn "Warning: No icon found. Using placeholder."
fi

# Also copy icon to standard location
mkdir -p "${APPDIR_NAME}/usr/share/icons/hicolor/256x256/apps"
if [ -f "${APPDIR_NAME}/${DESKTOP_FILE_NAME}.png" ]; then
    cp "${APPDIR_NAME}/${DESKTOP_FILE_NAME}.png" "${APPDIR_NAME}/usr/share/icons/hicolor/256x256/apps/${DESKTOP_FILE_NAME}.png"
fi

# Download appimagetool if not present
APPIMAGETOOL="appimagetool-x86_64.AppImage"
if [ ! -f "${APPIMAGETOOL}" ]; then
    echo "Downloading appimagetool..."
    wget -q --show-progress "${APPIMAGE_TOOL_URL}" -O "${APPIMAGETOOL}"
    chmod +x "${APPIMAGETOOL}"
fi

log.success "✓ AppDir created"

# Build AppImage
OUTPUT_NAME="OONI-Probe-${VERSION}-x86_64.AppImage"
log.lifecycle "Building AppImage: ${OUTPUT_NAME}"

ARCH=x86_64 "./${APPIMAGETOOL}" --no-appstream "${APPDIR_NAME}" "${OUTPUT_NAME}"

if [ $? -eq 0 ] && [ -f "${OUTPUT_NAME}" ]; then
    echo ""
    log.success "AppImage created: ${WORKSPACE}/${OUTPUT_NAME}"

    # Make it executable
    chmod +x "${OUTPUT_NAME}"

    # Get file size
    SIZE=$(du -h "${OUTPUT_NAME}" | cut -f1)
    log.info "Size: ${SIZE}"

    # Calculate SHA256
    log.info "Calculating SHA256 checksum..."
    sha256sum "${OUTPUT_NAME}" > "${OUTPUT_NAME}.sha256"
    log.success "Checksum saved to: ${OUTPUT_NAME}.sha256"
    cat "${OUTPUT_NAME}.sha256"

    log.info "To test the AppImage, run:"
    log.highlight "${WORKSPACE}/${OUTPUT_NAME}"
    log.info "To move it to the project dist directory:"
    log.highlight "mkdir -p ${PROJECT_ROOT}/dist && mv ${WORKSPACE}/${OUTPUT_NAME}* ${PROJECT_ROOT}/dist/"
else
    log.error "Error: Failed to create AppImage"
    exit 1
fi

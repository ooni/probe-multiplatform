# Desktop Update System Setup

This document describes how to set up and use the Sparkle (macOS) and WinSparkle (Windows) update system for the OONI Probe desktop application.

## Overview

The desktop application uses native update frameworks:
- **macOS**: Sparkle framework for automatic updates
- **Windows**: WinSparkle library for automatic updates
- **Linux**: No automatic updates (manual updates only)

## Prerequisites

### macOS (Sparkle)
1. **Sparkle Framework**: Download from [Sparkle Releases](https://github.com/sparkle-project/Sparkle/releases/latest)
2. **Xcode Command Line Tools**: `xcode-select --install`
3. **EdDSA Keys**: Generated using Sparkle's `generate_keys` tool

### Windows (WinSparkle)
1. **WinSparkle DLL**: Download from [WinSparkle Releases](https://github.com/vslavik/winsparkle/releases/latest)
2. **Visual Studio Build Tools** or **MinGW**

## Setup Instructions

### 1. Download Dependencies

#### macOS
```bash
# Download and extract Sparkle framework
curl -L https://github.com/sparkle-project/Sparkle/releases/latest/download/Sparkle-for-Swift-Package-Manager.zip -o sparkle.zip
unzip sparkle.zip
cp -r Sparkle.framework /path/to/project/
```

#### Windows
```bash
# Download WinSparkle DLL
curl -L https://github.com/vslavik/winsparkle/releases/latest/download/WinSparkle-0.7.0.zip -o winsparkle.zip
unzip winsparkle.zip
cp WinSparkle.dll /path/to/project/
```

### 2. Generate Signing Keys (macOS only)

```bash
# Navigate to Sparkle tools
cd Sparkle.framework/Versions/B/Resources/
./generate_keys

# This will output your public key - copy it to conveyor.conf
# Example output:
# SUPublicEDKey: pfIShU4dEXqPd5ObYNfDBiQWcXozk7estwzTnF9BamQ=
```

### 3. Update Configuration

Edit `conveyor.conf` and replace `REPLACE_WITH_YOUR_SPARKLE_PUBLIC_KEY` with your actual EdDSA public key.

### 4. Build Native Libraries

```bash
cd composeApp/src/desktopMain
make clean
make all
```

This builds:
- `libnetworktypefinder.dylib` / `networktypefinder.dll`
- `libupdatebridge.dylib` / `updatebridge.dll`

### 5. Build Application

```bash
./gradlew clean
./gradlew :composeApp:packageDistributionForCurrentOS
```

## Appcast Feed

The update system requires an appcast feed (RSS-like XML) containing update information.

### Example Appcast Structure

```xml
<?xml version="1.0" encoding="utf-8"?>
<rss version="2.0" xmlns:sparkle="http://www.andymatuschak.org/xml-namespaces/sparkle">
    <channel>
        <title>OONI Probe Updates</title>
        <description>Updates for OONI Probe Desktop</description>
        <language>en</language>
        <item>
            <title>OONI Probe 5.1.1</title>
            <description><![CDATA[
                <h2>Bug Fixes</h2>
                <ul>
                    <li>Fixed update mechanism</li>
                    <li>Improved network detection</li>
                </ul>
            ]]></description>
            <pubDate>Mon, 28 Jul 2025 10:00:00 +0000</pubDate>
            <enclosure
                url="https://github.com/ooni/probe-desktop/releases/download/v3.10.0/OONI-Probe-3.10.0.dmg"
                type="application/octet-stream"
                sparkle:version="5.1.1"
                sparkle:shortVersionString="5.1.1"
                sparkle:edSignature="4KzqXOTJen0K4lJKnEoL8oeMCgT8iXNLLxiSndtMXq4EmG9gJdBM0VUT8t4u71E+PYegIBy4VaGhhUQ/wBnj8Q=="
                sparkle:os="macos" />
            <enclosure
                url="https://github.com/ooni/probe-desktop/releases/download/v3.10.0/OONI-Probe-Setup-3.10.0.exe"
                type="application/octet-stream"
                sparkle:version="5.1.1"
                sparkle:shortVersionString="5.1.1"
                sparkle:edSignature="5hPmN/ovfiML7JxGQ3ao8giLXfld6UlqsuMmdxB4CoPd2acmEEkcYn/Fnv2QcRGejKQnQ/uy80Bhufg9BW9Vew=="
                sparkle:os="windows" />
        </item>
    </channel>
</rss>
```

### Signing Updates

#### macOS (EdDSA)
```bash
# Sign the update archive
./sign_update /path/to/ooni-probe-5.1.1-mac.dmg
```

#### Windows (EdDSA)
```bash
# Sign the update archive
./sign_update /path/to/ooni-probe-5.1.1-win.exe
```

## Testing

### Test Update Process

1. **Build older version**: Edit `CFBundleVersion` in Info.plist to a lower version
2. **Run application**: Launch the app
3. **Trigger update check**: Use "Check for Updates..." menu item
4. **Verify behavior**: Ensure update dialog appears and process works

### Manual Testing Commands

```bash
# Test on macOS
defaults delete org.ooni.probe SULastCheckTime
./OONI\ Probe.app/Contents/MacOS/OONI\ Probe

# Test on Windows
reg delete "HKCU\Software\OONI\OONI Probe\WinSparkle" /v LastCheckTime /f
./ooni-probe.exe
```

## Configuration Options

### UpdateManager Settings

```kotlin
// In Main.kt
updateManager.initialize("https://api.ooni.org/api/v1/check_updates")
updateManager.setAutomaticUpdatesEnabled(true)
updateManager.setUpdateCheckInterval(24) // Hours
```

### Conveyor Configuration

```hocon
// conveyor.conf
app {
    mac.info-plist {
        SUFeedURL = "https://api.ooni.org/api/v1/check_updates"
        SUPublicEDKey = "your-public-key-here"
        SUAutomaticallyUpdate = false  // Manual approval
        SUScheduledCheckInterval = 86400  // 24 hours
    }
}
```

## Deployment

### CI/CD Integration

1. **Build and sign** application packages
2. **Generate appcast** with signed update information
3. **Upload artifacts** to release server
4. **Update appcast feed** with new version info

### Release Workflow

```yaml
# Example GitHub Actions workflow
- name: Build Desktop App
  run: ./gradlew packageDistributionForCurrentOS

- name: Sign Updates
  run: |
    ./sign_update dist/*.dmg > signature-mac.txt
    ./sign_update dist/*.exe > signature-win.txt

- name: Generate Appcast
  run: ./generate_appcast releases/

- name: Upload Release
  uses: actions/upload-artifact@v3
  with:
    name: desktop-release
    path: |
      dist/
      appcast.xml
```

## Troubleshooting

### Common Issues

1. **Library loading errors**: Ensure native libraries are in correct path
2. **Signature verification failures**: Check EdDSA keys and signatures
3. **Network errors**: Verify appcast URL accessibility
4. **Permission issues**: Check code signing and notarization (macOS)

### Debug Output

```bash
# Enable verbose logging (macOS)
defaults write org.ooni.probe SUEnableLogging -bool YES

# Check Windows event logs
eventvwr.msc
```

## Security Considerations

- **EdDSA private keys**: Store securely, never in version control
- **HTTPS enforcement**: All update traffic must use TLS
- **Code signing**: Maintain valid Developer ID certificates
- **Appcast security**: Use HTTPS for appcast feed
- **Update verification**: Both Sparkle and WinSparkle verify signatures

## Migration from Conveyor Updates

1. **Phase 1**: Deploy with both systems enabled
2. **Phase 2**: Test with beta users
3. **Phase 3**: Disable Conveyor updates, use native only
4. **Phase 4**: Remove Conveyor update configuration

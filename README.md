# OONI Probe Multiplatform App

[![OONI Probe Android](https://github.com/ooni/probe/blob/master/images/OONIProbeLogo.png?raw=true)](https://ooni.org)

<p align="center">
  <a href="https://slack.openobservatory.org/"><img alt="slack" src="https://slack.openobservatory.org/badge.svg"/></a>
  <a href="https://github.com/ooni/probe-multiplatform/issues"><img alt="open issues" src="https://img.shields.io/github/issues/ooni/probe-multiplatform"/></a>
  <a href="https://mastodon.social/@ooni"><img alt="Mastodon Follow" src="https://img.shields.io/mastodon/follow/1106799?domain=mastodon.social&style=flat&label=mastodon"/></a>
  <a href="https://x.com/OpenObservatory"><img alt="X (formerly Twitter) Follow" src="https://img.shields.io/twitter/follow/openobservatory?style=flat&label=X%20(formerly%20Twitter)"/></a>
  <a href="https://bsky.app/profile/ooni.org"><img alt="Bluesky followers" src="https://img.shields.io/bluesky/followers/ooni.org?style=flat&label=bluesky"/></a>
</p>

Multiplatform (Android, iOS and Desktop) version of the Probe app.

**Releases**

[![Probe Android @ Google Play](https://img.shields.io/endpoint?color=2D638B&logo=google-play&logoColor=8DD8F8&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dorg.openobservatory.ooniprobe%26gl%3DUS%26hl%3Den%26l%3DProbe%2520Android%2520%2540%2520Google%2520Play%26m%3D%24version)](https://play.google.com/store/apps/details?id=org.openobservatory.ooniprobe)<br/>
[![Probe Android @ F-Droid](https://img.shields.io/f-droid/v/org.openobservatory.ooniprobe?logo=fdroid&logoColor=8DD8F8&label=Probe%20Android%20%40%20F-Droid&color=2D638B)](https://f-droid.org/en/packages/org.openobservatory.ooniprobe/)<br/>
[![Probe iOS @ App Store](https://img.shields.io/itunes/v/1199566366?logo=appstore&logoColor=8DD8F8&label=Probe%20iOS%20%40%20App%20Store&color=2D638B)](https://apps.apple.com/us/app/ooni-probe/id1199566366)<br/>
[![Probe Desktop @ App Store](https://img.shields.io/itunes/v/6762281670?logo=appstore&logoColor=8DD8F8&label=Probe%20Desktop%20%40%20App%20Store&color=2D638B)](https://apps.apple.com/app/ooni-probe-desktop/id6762281670) <br/>
[![NMS Android @ Google Play](https://img.shields.io/endpoint?color=D32625&logo=google-play&logoColor=D32625&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dcom.dw.ooniprobe%26gl%3DDE%26hl%3Den%26l%3DNMS%2520Android%2520%2540%2520Google%2520Play%26m%3D%24version)](https://play.google.com/store/apps/details?id=com.dw.ooniprobe)</br>
[![NMS iOS @ App Store](https://img.shields.io/itunes/v/6738992797?logo=appstore&logoColor=D32625&label=NMS%20iOS%20%40%20App%20Store&color=D32625)](https://apps.apple.com/us/app/news-media-scan/id6738992797)

**CI Status**

[![Validate](https://github.com/ooni/probe-multiplatform/actions/workflows/validate.yml/badge.svg)](https://github.com/ooni/probe-multiplatform/actions/workflows/validate.yml)
[![Android Instrumented Tests](https://github.com/ooni/probe-multiplatform/actions/workflows/run_android_instrumented_tests.yml/badge.svg)](https://github.com/ooni/probe-multiplatform/actions/workflows/run_android_instrumented_tests.yml)

## Project structure

* `composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - `androidMain` Android-specific code
  - `iosMain` iOS-specific code written in Kotlin
  - `desktopMain` Desktop-specific code
  - `dwMain` News Media Scan specific Branding and customization
  - `ooniMain` OONI Probe specific Branding and customization

  See [`docs/Distribution.md`](docs/Distribution.md) for a full comparison of
  the `ooni`/`dw` organizations, the desktop distribution channels, and the
  Apple App Store-specific restrictions.

* `iosApp` contains the iOS application configuration and the engine integration written in Swift

* `.github` contains the Continuous Integration configuration for Github

* `gradle/libs.versions.toml` specifies the versions of the plugins and dependencies used across
  the different modules.

## Build, Install, and Run

To build, install, and run your application, use the following commands:

- For OONI Probe:
  ```
  ./gradlew runDebug -Porganization=ooni
  ```

- For News Media Scan:
  ```
  ./gradlew runDebug -Porganization=dw
  ```

There is a custom gradle task(`copyBrandingToCommonResources`) that is used to copy brand specific
resources to the common resources folder. This task is called before the `preBuild` task.

## Creating Run Configurations in Android Studio

Configure run configurations for easy execution within Android Studio:

### OONI Probe Android Configuration

1. Click the **Plus (+)** sign in the top left corner of the "Run/Debug Configurations" dialog.
2. Choose 'Gradle'.
3. Configure with the following:
- **Name:** OONI_Probe
- **Run:** :composeApp:runDebug -Porganization=ooni

### News Media Scan Android Configuration

1. Repeat the steps for creating a new configuration.
2. Configure with the following:
- **Name:** News_Media_Scan
- **Run:** :composeApp:runDebug -Porganization=dw

### iOS setup

iOS dependencies are Swift packages declared in `composeApp/build.gradle.kts`
(`swiftPMDependencies`); there is no `pod install` step. Open `iosApp/iosApp.xcodeproj` in Xcode.
The Kotlin Gradle Plugin generates `iosApp/KotlinMultiplatformLinkedPackage`, and the Xcode project
references it as a local package to link every Swift package into the app. Commit it, but don't
edit it by hand.

#### Changing SwiftPM dependencies

After you edit `swiftPMDependencies`, the next Xcode build regenerates
`KotlinMultiplatformLinkedPackage` and fails once with *"Synthetic linkage package files changed
during the build"*. Select File > Packages > Resolve Package Versions in Xcode, or build again.
Then commit the regenerated files, together with:

- `.swiftpm-locks/default/swiftImport/Package.resolved`, the versions Kotlin compiles against;
- `iosApp/iosApp.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved`, the
  versions Xcode links.

The rest of `.swiftpm-locks/` is regenerated on every build and is git-ignored.

#### Build settings to keep

- `ENABLE_USER_SCRIPT_SANDBOXING = NO` (`iosApp/Configuration/Config.xcconfig`). The *Compile
  Kotlin Framework* phase runs Gradle, which reads and writes outside the sandbox.
- `-ObjC` in `OTHER_LDFLAGS` of the app targets. It loads the Objective-C categories in the
  static libraries the app links.
- `FRAMEWORK_SEARCH_PATHS` and `-framework composeApp` in the UI test targets. They import
  `composeApp` but don't run the *Compile Kotlin Framework* phase, so they find the framework that
  the app target's build leaves in `composeApp/build/xcode-frameworks/`.

### Updating oonimkall and Passport

Android and Desktop take both libraries from Maven Central. Bump `oonimkall` and `passport` in
`gradle/libs.versions.toml`.

iOS uses local Swift packages in `iosApp/Packages/`:

| Package   | Source                                                                                   | Update with                                               |
|-----------|------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| oonimkall | [ooni/probe-cli `rolling`](https://github.com/ooni/probe-cli/releases/tag/rolling)       | `scripts/update-ios-oonimkall-package.sh [tag]`           |
| Passport  | [ooni/ooniprobe-rs `rolling`](https://github.com/ooni/ooniprobe-rs/releases/tag/rolling) | `scripts/update-ios-passport-package.sh [tag \| release]` |

- **Passport** follows the ooniprobe-rs `rolling` release. The script copies the release's
  `Package.swift` and its matching UniFFI bindings (`ooniprobe.swift`), and fails if the release
  doesn't publish both. To pin a release, pass its tag (`v0.1.9`), or `release` for the tag of
  `passport` in `gradle/libs.versions.toml`, the version Android and Desktop use. Releases up to
  v0.1.8 don't publish these files.
- **oonimkall** follows the probe-cli `rolling` release, which publishes a `Package.swift`.
  Tagged probe-cli releases up to v3.31.1 don't publish one, and their native libraries can't be
  code-signed by Xcode. Pass a tag to the script once a probe-cli release ships `Package.swift`.

To update:

1. Run the script.
2. In Xcode, select File > Packages > Reset Package Caches, then build.
3. Commit the changed files in `iosApp/Packages/`.

The `rolling` release is overwritten on every upstream build, which changes the artifact
checksums. When Xcode reports a checksum mismatch for oonimkall or Passport, run its script again. Swift
Package Manager also caches artifacts by URL. If the mismatch persists after resetting the package
caches, delete the stale entries and build again:

```
rm -rf ~/Library/Caches/org.swift.swiftpm/artifacts/*probe_cli_releases_download_rolling*
rm -rf ~/Library/Caches/org.swift.swiftpm/artifacts/*ooniprobe_rs_releases_download_rolling*
```

Gradle keeps its own Xcode build of the Swift packages in `build/kotlin/swiftPMXcodeDumps/`. When
a package's headers change in place (for example new Passport bindings), the *Compile Kotlin
Framework* phase can fail with *"file '…/ooniprobeFFI.h' has been modified since the module file
… was built"* or *"cannot find 'uniffi_…' in scope"*. Delete that folder and build again:

```
rm -rf build/kotlin/swiftPMXcodeDumps
```

### OONI Probe iOS Configuration

The "Run/Debug Configurations" already has the proper configuration and you just need to select the
XCode Project Scheme `OONIProbe` and run it.

### News Media Scan iOS Configuration

The "Run/Debug Configurations" already has the proper configuration and you just need to select the
XCode Project Scheme `NewsMediaScan` and run it.

### Switching between OONI Probe and News Media Scan

- Ensure you can run clean and build the project successfully.

## Testing

Common tests (tests inside `commonTest`) only run on the iOS Simulator.
Choosing the option `android (local)` won't work. This is a current
[issue](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html#f03e048) with
the official testing library.

## Translations

[![Translation status](https://localizationlab.weblate.cloud/widget/ooni/app-strings-common-xml/287x66-grey.png)](https://localizationlab.weblate.cloud/engage/ooni/)

The app is translated by the community through [Localization Lab's Weblate](https://localizationlab.weblate.cloud/projects/ooni/probe/)
([Guidelines](https://github.com/ooni/translations/blob/master/Guidelines%20for%20OONI%20Probe.md)).

**Important Note:** Any Pull Request that introduces new strings into the app should be
reviewed by @agrabeli. The Pull Request should include screenshots showing the strings in context.

New or updated strings on the default version of the `strings-common.xml` and
`strings-organization.xml` files are automatically uploaded to Weblate when a Pull Request is
merged into the `main` branch.

Weblate automatically creates Pull Requests with the latest translation changes to keep the app
up-to-date.

### Metadata

Metadata such as Store Listing copy and screenshots are located in the `metadata` root folder.
Weblate also maintains those translations automatically.

Inside `fastlane/metadata` are only symbolic links mapping the locales supported by Google Play or
Apple App Store to the Weblate locales. Changes should only be made to the root `metadata` folder.

# OONI Probe Multiplatform App

Multiplatform (Android and iOS currently) version of the Probe app.

**Releases**

[![Probe Android @ Google Play](https://img.shields.io/endpoint?color=2D638B&logo=google-play&logoColor=8DD8F8&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dorg.openobservatory.ooniprobe%26gl%3DUS%26hl%3Den%26l%3DProbe%2520Android%2520%2540%2520Google%2520Play%26m%3D%24version)](https://play.google.com/store/apps/details?id=org.openobservatory.ooniprobe)
[![NMS Android @ Google Play](https://img.shields.io/endpoint?color=D32625&logo=google-play&logoColor=D32625&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dcom.dw.ooniprobe%26gl%3DDE%26hl%3Den%26l%3DNMS%2520Android%2520%2540%2520Google%2520Play%26m%3D%24version)](https://play.google.com/store/apps/details?id=com.dw.ooniprobe)

**CI Status**

[![Validate](https://github.com/ooni/probe-multiplatform/actions/workflows/validate.yml/badge.svg)](https://github.com/ooni/probe-multiplatform/actions/workflows/validate.yml)
[![Android Instrumented Tests](https://github.com/ooni/probe-multiplatform/actions/workflows/instrumented-tests.yml/badge.svg)](https://github.com/ooni/probe-multiplatform/actions/workflows/instrumented-tests.yml)

## Project structure

* `composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - `androidMain` Android-specific code
  - `iosMain` iOS-specific code written in Kotlin
  - `dwMain` News Media Scan specific Branding and customization
  - `ooniMain` OONI Probe specific Branding and customization

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

### OONI Probe iOS Configuration

The "Run/Debug Configurations" already has the proper configuration and you just need to select the
XCode Project Scheme `OONIProbe` and run it.

### News Media Scan iOS Configuration

The "Run/Debug Configurations" already has the proper configuration and you just need to select the
XCode Project Scheme `NewsMediaScan` and run it.

### Switching between OONI Probe and News Media Scan

- Ensure you can run clean and build the project successfully.
- Run `pod install` in the `iosApp` directory.

## Testing

Common tests (tests inside `commonTest`) only run on the iOS Simulator.
Choosing the option `android (local)` won't work. This is a current
[issue](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html#f03e048) with
the official testing library.

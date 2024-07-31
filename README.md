# OONI Probe Multiplatform

Multiplatform (Android and iOS currently) version of the Probe app.

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

## Architecture overview

Our aim is to take advantage of multiplatform features as much as possible, specially [Compose
Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/). Platform specific modules
will be limited to the minimum required to setup and launch the apps with a compose wrapper,
besides platform-specific code that we can’t avoid, such as the loading our pre-compiled engine.

### Principals

* [Dependency Inversion](https://developer.android.com/topic/modularization/patterns#dependency_inversion)
* [Unidirectional Data Flow](https://developer.android.com/develop/ui/compose/architecture#udf)
* [Model-View-ViewModel](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-viewmodel.html)

### Main module structure

* `engine` the Oonimkall engine abstraction in kotlin

* `probe` our Probe app code

  * `di` dependency injection
  * `shared` classes and methods shared across the whole app
  * `data` data layer code (database, preferences, network...)
  * `ui` UI layer code, organized into features/screens



### Build, Install, and Run

To build, install, and run your application, use the following commands:

- For OONI Probe:
  ```
  ./gradlew runDebug -Porganization=ooni
  ```

- For News Media Scan:
  ```
  ./gradlew runDebug -Porganization=dw
  ```

There is a custom gradle task(`copyBrandingToCommonResources`) that is used to copy brand specific resources to the common resources folder. This task is called before the `preBuild` task.

### Creating Run Configurations in Android Studio

Configure run configurations for easy execution within Android Studio:

#### OONI Probe Android Configuration

1. Click the **Plus (+)** sign in the top left corner of the "Run/Debug Configurations" dialog.
2. Choose 'Gradle'.
3. Configure with the following:
- **Name:** OONI_Probe
- **Run:** :composeApp:runDebug -Porganization=ooni

#### News Media Scan Android Configuration

1. Repeat the steps for creating a new configuration.
2. Configure with the following:
- **Name:** News_Media_Scan
- **Run:** :composeApp:runDebug -Porganization=dw

#### OONI Probe iOS Configuration

The "Run/Debug Configurations" already has the proper configuration and you just need to select the XCode Project Scheme `OONIProbe` and run it.

#### News Media Scan iOS Configuration
The "Run/Debug Configurations" already has the proper configuration and you just need to select the XCode Project Scheme `NewsMediaScan` and run it.

#### Switching between OONI Probe and News Media Scan
- Ensure you can run clean and build the project successfully.
- Run `pod install` in the `iosApp` directory.

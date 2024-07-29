# OONI Probe Multiplatform

Multiplatform (Android and iOS currently) version of the Probe app.

## Project structure

* `composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - `androidMain` Android-specific code
  - `iosMain` iOS-specific code written in Kotlin

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

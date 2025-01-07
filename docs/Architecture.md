# Architecture

Our aim is to take advantage of multiplatform features as much as possible, specially [Compose
Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/). Platform specific modules
will be limited to the minimum required to setup and launch the apps with a compose wrapper,
besides platform-specific code that we can’t avoid, such as the loading our pre-compiled engine.

## Principals

* [Dependency Inversion](https://developer.android.com/topic/modularization/patterns#dependency_inversion)
* [Unidirectional Data Flow](https://developer.android.com/develop/ui/compose/architecture#udf)
* [Model-View-ViewModel](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-viewmodel.html)

## Main module structure

* `engine` the Oonimkall engine abstraction in kotlin

* `probe` our Probe app code

    * `background` shared code ran on background tasks
    * `config` configurations for each platform (Android, iOS), organization (OONI, DW) and flavor (full, F-droid)
    * `di` dependency injection
    * `shared` classes and methods shared across the whole app
    * `data` data layer code (database, preferences, network...)
    * `ui` UI layer code, organized into features/screens

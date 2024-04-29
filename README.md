# OONI Probe Multiplatform

The goal of this repo is to outline a proposed architecture for building a cross platform app that 
targets Android, iOS and Desktop (windows and macOS).

The idea is not to use this project as-is, but rather use it as a reference and playground to 
experiment with design pattern related to iteratively refactoring OONI Probe Android, iOS and Desktop 
under a unified code base.

### Project structure

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.
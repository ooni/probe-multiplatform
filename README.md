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

### Architecture overview

To best understand the architecture it's best you look at the commit history:

#### kmp boilerplate
commit: https://github.com/ooni/probe-multiplatform/commit/e8f2f6dc4f09f15679e064e5350be257e5de9335

nothing really to see here, this is just the output, as-is of
https://kmp.jetbrains.com/:

### general app architecture

commit: https://github.com/ooni/probe-multiplatform/commit/917e92c4689e6ee664a36a7b9266d56422257e1b

this is where all the setup of the architecture of the app is done to create a
structure that should be relatively scalable and modular to support the
specific cross platform constraints we have in our app

### golang bridging
commit: https://github.com/ooni/probe-multiplatform/commit/38f95f35223808f3d531a4690458776645c02105

this is where the golang bridging actually happens.

The build of the library is actually done inside of gradle steps using a combination of cmake + Makefiles as part of building the app.

The relevant bits to do this are here:
* main gradle entry point: https://github.com/ooni/probe-multiplatform/commit/38f95f35223808f3d531a4690458776645c02105#diff-9ea83bf74425e7270f5dd[%E2%80%A6]feb671f9578fabdec009eb4ba4a
* cmake config references from gradle: https://github.com/ooni/probe-multiplatform/commit/38f95f35223808f3d531a4690458776645c02105#diff-ee1cbd25a6321e45f790ea552825ea601d5ac9a6233aaba6e3e71143957985cd
* Makefile doing the actual build: https://github.com/ooni/probe-multiplatform/commit/38f95f35223808f3d531a4690458776645c02105#diff-ac4f5f32e2945fdd5a37c125502d77bb5ccdebc1d9797469916295930d587f01

The JNI is built by hand instead of relying on gomobile. The reason for this is
that gomobile doesn't actually work that well (it doesn't support complex
types), so instead we build a very minimal bridge API surface that can then be
mapped to correct types directly inside of kotlin.

* Specifically the mobile API has only 2 functions: apiCall and apiCallWithArgs: https://github.com/ooni/probe-multiplatform/commit/38f95f35223808f3d531a4690458776645c02105#diff-a18d06043032f63c9ef45e5de6fd5a014d533786993260306511f2fe0135f070R134
* These two functions are mapped using the JNI and linked into the native GoOONIProbeClient:  https://github.com/ooni/probe-multiplatform/commit/38f95f35223808f3d531a4690458776645c02105#diff-68b2196fd77caf703a289903ce6a1b5d03a167c774d5a6150fb4c9bf734c0228
* There is then a bridge which actually instantiates the OONIProbeClient and calls the static methods on top of it: https://github.com/ooni/probe-multiplatform/commit/38f95f35223808f3d531a4690458776645c02105#diff-0160d602dbe85af66fc75f7f96003f6cd0129f965a6a39636117702deebc1ce5
    - The reason to use a bridge instead of calling OONIProbeClient directly is that we need to be able to inject dependencies at runtime to swap out the native implementation for each platform
* What a mobile app developers ends up using, in the end, is the nicer typed
  interface which uses the bridge to call the native functions and handles the serialization/deserialization of function call arguments and return values from the native calls: https://github.com/ooni/probe-multiplatform/commit/38f95f35223808f3d531a4690458776645c02105#diff-799618a943c0407c70266082ddf3882bd252160bbc83a135d999df074d4109d9

What will eventually be calling probe-cli under the hood would live in here:
https://github.com/ooni/probe-multiplatform/commit/38f95f35223808f3d531a4690458776645c02105#diff-a18d06043032f63c9ef45e5de6fd5a014d533786993260306511f2fe0135f070

What is inside of the BEGIN API section should all be replaced with just an
import from `github.com/ooni/probe-engine` which should export a type API
struct which lives somewhere inside of a `mobileapi` package that implements
the `Call`, `CallWithArgs` and `Init` methods.

The shim code on the other hand can and probably should live directly inside of
the probe codebase so that it's as easy to change as possible and understand
the bridging layer properly.

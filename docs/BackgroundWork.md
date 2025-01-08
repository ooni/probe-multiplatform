# Background Work

The Probe Multiplatform App has two different background-tasks:

**1. Running Tests**

Manual and automatic running of descriptor tests.

When:
* If auto-run is enabled, it runs roughly every hour, if the constraint the user picked in the
  settings apply (only on WiFi or when device is charging). It runs with a specification based on
  the descriptors and tests enabled for auto-run.
* It can be triggered manually when the user runs tests inside the app, with the specification they
  picked.

**2. Descriptor Update**

Fetch all the installed descriptors to check if any has a more up-to-date version. If they do, it
either updates automatically the descriptor, or warns the user that there's an update to approve.

When:
* App start
* If the user pulls-down to refresh on the Dashboard screen, or a specific Descriptor screen
* Once per day, if the device if connected to any Network.

## Implementation

### Android

Work is scheduled and ran using [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) ([source](https://github.com/ooni/probe-multiplatform/blob/main/composeApp/src/androidMain/kotlin/org/ooni/probe/background/AppWorkerManager.kt)).

### iOS

Work is scheduled using [BGTaskScheduler](https://developer.apple.com/documentation/UIKit/using-background-tasks-to-update-your-app) ([source](https://github.com/ooni/probe-multiplatform/blob/main/composeApp/src/iosMain/kotlin/org/ooni/probe/background/OperationsManager.kt)).
It runs in a background thread using [DispatchQueue](https://developer.apple.com/documentation/dispatch/dispatchqueue) ([source](https://github.com/ooni/probe-multiplatform/blob/main/iosApp/iosApp/background/IosBackgroundRunner.swift)).

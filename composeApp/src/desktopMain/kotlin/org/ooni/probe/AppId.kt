package org.ooni.probe

// Desktop application id. Lives in :composeApp because the desktop dependency
// graph (BuildDependencies) and InstanceManager use it; the :desktopApp entry
// point (Main.kt) references it from the same package.
const val APP_ID = "org.ooni.probe"

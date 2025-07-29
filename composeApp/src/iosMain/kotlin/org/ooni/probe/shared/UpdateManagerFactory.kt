package org.ooni.probe.shared

actual fun createUpdateManager(platform: Platform): UpdateManager = NoOpUpdateManager()

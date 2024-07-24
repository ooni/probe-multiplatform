package org.ooni.probe

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.ooni.engine.DesktopOonimkallBridge
import org.ooni.probe.di.Dependencies

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "OONI Probe",
    ) {
        App(Dependencies(DesktopOonimkallBridge(), ""))
    }
}
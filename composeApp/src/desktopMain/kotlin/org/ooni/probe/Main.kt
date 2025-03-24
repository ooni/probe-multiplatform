package org.ooni.probe

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "GoDesktop",
        ) {
            App(
                dependencies = dependencies,
                deepLink = null,
                onDeeplinkHandled = {},
            )
        }
    }
}

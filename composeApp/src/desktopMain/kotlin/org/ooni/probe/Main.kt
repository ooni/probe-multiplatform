package org.ooni.probe

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
        ) {
            App(
                dependencies = dependencies,
                deepLink = null,
                onDeeplinkHandled = {},
            )
        }
    }
}

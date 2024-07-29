package org.ooni.probe

import androidx.compose.ui.window.ComposeUIViewController
import org.ooni.engine.OonimkallBridge
import org.ooni.probe.di.Dependencies
import platform.Foundation.NSTemporaryDirectory

fun mainViewController(bridge: OonimkallBridge) =
    ComposeUIViewController {
        App(Dependencies(bridge, NSTemporaryDirectory()))
    }

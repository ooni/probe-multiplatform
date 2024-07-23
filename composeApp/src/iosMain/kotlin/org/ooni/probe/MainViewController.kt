package org.ooni.probe

import androidx.compose.ui.window.ComposeUIViewController
import org.ooni.engine.OoniEngine
import org.ooni.probe.di.Dependencies

fun MainViewController(ooniEngine: OoniEngine) =
    ComposeUIViewController { App(Dependencies(ooniEngine)) }
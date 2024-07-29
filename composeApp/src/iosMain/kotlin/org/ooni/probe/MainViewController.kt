package org.ooni.probe

import androidx.compose.ui.window.ComposeUIViewController
import org.ooni.probe.di.Dependencies

fun mainViewController(dependencies: Dependencies) =
    ComposeUIViewController {
        App(dependencies)
    }

package org.ooni.probe

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.flow.SharedFlow
import org.ooni.probe.data.models.DeepLink
import org.ooni.probe.di.Dependencies
import platform.UIKit.UIViewController

fun mainViewController(
    dependencies: Dependencies,
    deepLinkFlow: SharedFlow<DeepLink>,
): UIViewController {
    return ComposeUIViewController {
        val deepLink by deepLinkFlow.collectAsState(null)
        App(
            dependencies = dependencies,
            deepLink = deepLink,
        )
    }
}

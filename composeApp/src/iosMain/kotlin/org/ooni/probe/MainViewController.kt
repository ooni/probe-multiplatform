package org.ooni.probe

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.flow.SharedFlow
import org.ooni.probe.data.models.DeepLink
import org.ooni.probe.di.Dependencies

fun mainViewController(
    dependencies: Dependencies,
    deepLinkFlow: SharedFlow<DeepLink>,
) = ComposeUIViewController {
    App(
        dependencies = dependencies,
        deepLink = deepLinkFlow.collectAsState(null).value,
    )
}

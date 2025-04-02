package org.ooni.probe

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.flow.MutableSharedFlow
import org.ooni.probe.data.models.DeepLink
import org.ooni.probe.di.Dependencies
import org.ooni.probe.ui.shared.requestAppReview
import platform.UIKit.UIViewController

@OptIn(ExperimentalComposeApi::class)
fun mainViewController(
    dependencies: Dependencies,
    deepLinkFlow: MutableSharedFlow<DeepLink?>,
): UIViewController {
    return ComposeUIViewController {
        val deepLink by deepLinkFlow.collectAsState(null)
        App(
            dependencies = dependencies,
            deepLink = deepLink,
            onDeeplinkHandled = {
                deepLink?.let {
                    deepLinkFlow.tryEmit(null)
                }
            },
        )

        LaunchedEffect(Unit) {
            if (dependencies.shouldShowAppReview()) {
                requestAppReview()
                dependencies.markAppReviewAsShown()
            }
        }
    }
}

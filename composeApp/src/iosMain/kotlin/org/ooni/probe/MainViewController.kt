package org.ooni.probe

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableSharedFlow
import org.ooni.probe.data.models.DeepLink
import org.ooni.probe.di.Dependencies
import org.ooni.probe.ui.shared.requestAppReview
import platform.UIKit.UIViewController

@OptIn(ExperimentalComposeApi::class)
fun mainViewController(
    dependencies: Dependencies,
    deepLinkFlow: MutableSharedFlow<DeepLink?>,
): UIViewController =
    ComposeUIViewController {
        val deepLink by deepLinkFlow.collectAsState(null)
        val languageTag = dependencies.localeController.currentLanguageTag()

        val navController = rememberNavController()
        key(languageTag) {
            App(
                dependencies = dependencies,
                deepLink = deepLink,
                onDeeplinkHandled = {
                    deepLink?.let {
                        deepLinkFlow.tryEmit(null)
                    }
                },
                navController = navController,
            )
        }

        LaunchedEffect(Unit) {
            if (dependencies.shouldShowAppReview()) {
                requestAppReview()
                dependencies.markAppReviewAsShown()
            }
        }
    }

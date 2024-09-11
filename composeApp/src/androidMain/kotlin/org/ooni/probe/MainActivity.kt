package org.ooni.probe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.DeepLink

class MainActivity : ComponentActivity() {
    private val deepLinkFlow = MutableSharedFlow<DeepLink?>(extraBufferCapacity = 1)
    private val app get() = applicationContext as AndroidApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val deepLink by deepLinkFlow.collectAsState(null)

            App(app.dependencies, deepLink)

            LaunchedEffect(intent) {
                intent?.let { manageIntent(it) }
            }
            LaunchedEffect(deepLink) {
                deepLink?.let {
                    // Reset the deepLinkFlow after processing the deep link
                    launch {
                        deepLinkFlow.emit(null)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        manageIntent(intent)
    }

    private fun manageIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        when (uri.host) {
            "runv2",
            OrganizationConfig.ooniRunDomain,
            -> {
                val id = uri.lastPathSegment ?: return
                deepLinkFlow.tryEmit(DeepLink.AddDescriptor(id))
            }
            else -> {
                Logger.e { "Unknown deep link: $uri" }
            }
        }
    }
}

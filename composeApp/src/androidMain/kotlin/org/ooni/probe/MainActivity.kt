package org.ooni.probe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.ooni.probe.data.models.DeepLink

class MainActivity : ComponentActivity() {
    private val deepLinkFlow = MutableSharedFlow<DeepLink>()
    private val app get() = applicationContext as AndroidApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App(app.dependencies, deepLinkFlow.asSharedFlow())
        }
        intent?.let { manageIntent(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_VIEW == intent.action) {
            manageIntent(intent)
        }
    }

    private fun manageIntent(intent: Intent) {
        intent.data?.let { uri: Uri ->
            try {
                when (uri.host) {
                    "runv2", "run.test.ooni.org" -> {
                        val id = uri.lastPathSegment ?: return
                        lifecycleScope.launch {
                            deepLinkFlow.emit(DeepLink.AddDescriptor(id))
                        }
                    } else -> {
                        Logger.e { "Unknown deep link: $uri" }
                    }
                }
            } catch (e: Exception) {
                Logger.e(e) { "Failed to open run v2 link" }
            }
        }
    }
}

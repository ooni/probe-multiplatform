package org.ooni.probe

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
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

            App(
                dependencies = app.dependencies,
                deepLink = deepLink,
                onDeeplinkHandled = {
                    deepLink?.let {
                        deepLinkFlow.tryEmit(null)
                    }
                },
            )

            LaunchedEffect(intent) {
                intent?.let { manageIntent(it) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        manageIntent(intent)
    }

    private fun manageIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW && intent.action != Intent.ACTION_SEND) return

        when (intent.action) {
            Intent.ACTION_VIEW -> {
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
            Intent.ACTION_SEND -> {
                val url = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                if (Patterns.WEB_URL.matcher(url).matches()) {
                    deepLinkFlow.tryEmit(DeepLink.RunUrls(url))
                } else {
                    return
                }
            }
        }
    }

    // Battery Optimization

    private var ignoreBatteryOptimizationCallback: (() -> Unit)? = null

    private val ignoreBatteryOptimizationContract =
        registerForActivityResult(object : ActivityResultContract<Unit, Unit>() {
            @SuppressLint("BatteryLife")
            override fun createIntent(
                context: Context,
                input: Unit,
            ) = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:$packageName"))

            override fun parseResult(
                resultCode: Int,
                intent: Intent?,
            ) {}
        }) { ignoreBatteryOptimizationCallback?.invoke() }

    fun requestIgnoreBatteryOptimization(callback: () -> Unit) {
        ignoreBatteryOptimizationCallback = callback
        ignoreBatteryOptimizationContract.launch(Unit)
    }
}

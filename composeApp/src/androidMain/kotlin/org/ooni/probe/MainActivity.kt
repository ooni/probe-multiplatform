package org.ooni.probe

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import org.ooni.probe.config.AndroidUpdateMonitoring
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.config.UpdateMonitoring
import org.ooni.probe.data.models.DeepLink

class MainActivity : ComponentActivity() {
    private val deepLinkFlow = MutableSharedFlow<DeepLink?>(extraBufferCapacity = 1)
    private val app get() = applicationContext as AndroidApplication
    private val updateMonitoring: UpdateMonitoring = AndroidUpdateMonitoring()

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

    override fun onResume() {
        super.onResume()
        updateMonitoring.onResume(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        manageIntent(intent)
    }

    private fun manageIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> manageOoniRun(intent)
            Intent.ACTION_SEND -> manageSend(intent)
            else -> return
        }
    }

    private fun manageSend(intent: Intent) {
        val url = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        deepLinkFlow.tryEmit(DeepLink.RunUrls(url))
    }

    private fun manageOoniRun(intent: Intent) {
        val uri = intent.data ?: return
        when (uri.host) {
            "runv2",
            OrganizationConfig.ooniRunDomain,
            -> {
                val id = uri.lastPathSegment ?: return
                deepLinkFlow.tryEmit(DeepLink.AddDescriptor(id))
            }

            else -> {
                deepLinkFlow.tryEmit(DeepLink.Error)
                Logger.e { "Unknown deep link: $uri" }
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

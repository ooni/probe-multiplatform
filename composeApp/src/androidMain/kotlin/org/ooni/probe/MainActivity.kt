package org.ooni.probe

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.ooni.engine.AndroidOonimkallBridge
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App(setupDependencies())
        }
    }

    private fun setupDependencies(): Dependencies {
        val platformInfo =
            object : PlatformInfo {
                override val version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                override val platform = Platform.Android
                override val osVersion = Build.VERSION.SDK_INT.toString()
                override val model = "${Build.MANUFACTURER} ${Build.MODEL}"
            }
        val bridge = AndroidOonimkallBridge()
        val dependencies =
            Dependencies(
                platformInfo = platformInfo,
                oonimkallBridge = bridge,
                baseFileDir = filesDir.absolutePath,
            )
        return dependencies
    }
}

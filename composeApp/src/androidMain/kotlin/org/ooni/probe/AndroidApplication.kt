package org.ooni.probe

import android.app.Application
import android.os.Build
import org.ooni.engine.AndroidOonimkallBridge
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo

class AndroidApplication : Application() {
    val dependencies by lazy {
        Dependencies(
            platformInfo = platformInfo,
            oonimkallBridge = AndroidOonimkallBridge(),
            baseFileDir = filesDir.absolutePath,
        )
    }

    private val platformInfo by lazy {
        object : PlatformInfo {
            override val version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            override val platform = Platform.Android
            override val osVersion = Build.VERSION.SDK_INT.toString()
            override val model = "${Build.MANUFACTURER} ${Build.MODEL}"
        }
    }
}

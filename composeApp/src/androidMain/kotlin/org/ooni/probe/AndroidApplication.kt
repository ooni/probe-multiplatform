package org.ooni.probe

import android.app.Application
import android.os.Build
import org.ooni.engine.AndroidOonimkallBridge
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo

/**
 * See link for `baseFileDir` https://github.com/ooni/probe-android/blob/5a11d1a36ec952aa1f355ba8db4129146139a5cc/engine/src/main/java/org/openobservatory/engine/Engine.java#L52
 * See link for `cacheDir` https://github.com/ooni/probe-android/blob/5a11d1a36ec952aa1f355ba8db4129146139a5cc/engine/src/main/java/org/openobservatory/engine/Engine.java#L70
 */
class AndroidApplication : Application() {
    val dependencies by lazy {
        Dependencies(
            platformInfo = platformInfo,
            oonimkallBridge = AndroidOonimkallBridge(),
            baseFileDir = filesDir.absolutePath,
            cacheDir = cacheDir.absolutePath,
            dataStore = getDataStore(this),
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

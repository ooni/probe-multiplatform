package org.ooni.probe

import org.ooni.engine.OonimkallBridge
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIDevice

/**
 * See link for `baseFileDir` https://github.com/ooni/probe-ios/blob/2145bbd5eda6e696be216e3bce97e8d5fb33dcea/ooniprobe/Engine/Engine.m#L54
 * See link for `cacheDir` https://github.com/ooni/probe-ios/blob/2145bbd5eda6e696be216e3bce97e8d5fb33dcea/ooniprobe/Engine/Engine.m#L66
 */
fun setupDependencies(bridge: OonimkallBridge) =
    Dependencies(
        platformInfo = platformInfo,
        oonimkallBridge = bridge,
        baseFileDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first().toString(),
        cacheDir = NSTemporaryDirectory(),
        dataStore = createDataStore(),
    )

private val platformInfo get() =
    object : PlatformInfo {
        override val version =
            (NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String).orEmpty()

        override val platform = Platform.Ios

        override val osVersion =
            with(UIDevice.currentDevice) {
                "$systemName $systemVersion"
            }

        override val model = UIDevice.currentDevice.model
    }

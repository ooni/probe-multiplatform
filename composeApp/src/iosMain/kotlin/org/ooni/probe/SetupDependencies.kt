package org.ooni.probe

import org.ooni.engine.OonimkallBridge
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import platform.Foundation.NSBundle
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIDevice

fun setupDependencies(bridge: OonimkallBridge) =
    Dependencies(
        platformInfo = platformInfo,
        oonimkallBridge = bridge,
        baseFileDir = NSTemporaryDirectory(),
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

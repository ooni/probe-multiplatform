package org.ooni.probe

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.OonimkallBridge
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.darwin.NSObject
import platform.darwin.NSObjectMeta

/**
 * See link for `baseFileDir` https://github.com/ooni/probe-ios/blob/2145bbd5eda6e696be216e3bce97e8d5fb33dcea/ooniprobe/Engine/Engine.m#L54
 * See link for `cacheDir` https://github.com/ooni/probe-ios/blob/2145bbd5eda6e696be216e3bce97e8d5fb33dcea/ooniprobe/Engine/Engine.m#L66
 */
fun setupDependencies(
    bridge: OonimkallBridge,
    networkTypeFinder: NetworkTypeFinder,
) = Dependencies(
    platformInfo = platformInfo,
    oonimkallBridge = bridge,
    baseFileDir =
        NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true,
        ).first().toString(),
    cacheDir = NSTemporaryDirectory(),
    readAssetFile = ::readAssetFile,
    databaseDriverFactory = ::buildDatabaseDriver,
    networkTypeFinder = networkTypeFinder,
    buildDataStore = ::buildDataStore,
    // TODO: isBatteryCharging
    isBatteryCharging = { true },
    launchUrl = ::launchUrl,
)

private val platformInfo
    get() =
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

private fun buildDatabaseDriver() = NativeSqliteDriver(schema = Database.Schema, name = "OONIProbe.db")

/*
 New asset files need to be added to the iOS project using xCode:
 - Right click iosApp where you want it and select "Add Files to..."
 - Pick `src/commonMain/resources`
 - Deselect "Copy items if needed" and select "Create groups"
 - Pick both targets OONIProbe and NewsMediaScan
 */
private fun readAssetFile(path: String): String {
    val fileName = path.split(".").first()
    val type = path.split(".").last()

    val resource = NSBundle.bundleForClass(BundleMarker).pathForResource(fileName, type)
    return resource?.let {
        NSString.stringWithContentsOfFile(resource) as? String
    } ?: run {
        error("Couldn't read asset file: $path")
    }
}

private class BundleMarker : NSObject() {
    companion object : NSObjectMeta()
}

fun buildDataStore(): DataStore<Preferences> =
    Dependencies.getDataStore(
        producePath = {
            val documentDirectory: NSURL? =
                NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null,
                )
            requireNotNull(documentDirectory).path + "/${Dependencies.Companion.DATA_STORE_FILE_NAME}"
        },
    )

private fun launchUrl(url: String) {
    NSURL.URLWithString(url)?.let {
        UIApplication.sharedApplication.openURL(it)
    }
}

package org.ooni.probe

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.nativeHeap.alloc
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.OonimkallBridge
import org.ooni.probe.background.RunOperation
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.DeepLink
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.di.Dependencies
import org.ooni.probe.domain.GetAutoRunSpecification
import org.ooni.probe.domain.RunDescriptors
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import platform.BackgroundTasks.BGProcessingTask
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dateByAddingTimeInterval
import platform.Foundation.stringWithContentsOfFile
import platform.MessageUI.MFMailComposeViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState
import platform.UIKit.UIPasteboard
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
    isBatteryCharging = ::checkBatteryCharging,
    launchUrl = ::launchUrl,
    configureAutoRun = ::configureAutoRun,
)

fun initializeDeeplink() = MutableSharedFlow<DeepLink>(extraBufferCapacity = 1)

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

private fun checkBatteryCharging(): Boolean {
    UIDevice.currentDevice.batteryMonitoringEnabled = true
    return UIDevice.currentDevice.batteryState == UIDeviceBatteryState.UIDeviceBatteryStateCharging
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

private fun launchUrl(
    url: String,
    extras: Map<String, String>?,
) {
    NSURL.URLWithString(url)?.let {
        if (it.scheme == "mailto") {
            MFMailComposeViewController.canSendMail().let { canSendMail ->
                val email = it.toString().removePrefix("mailto:")
                if (canSendMail) {
                    MFMailComposeViewController().apply {
                        setToRecipients(listOf(email))
                        extras?.forEach { (key, value) ->
                            when (key) {
                                "subject" -> setSubject(value)
                                "body" -> setMessageBody(value, isHTML = false)
                            }
                        }
                    }.let {
                        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(it, true, null)
                    }
                } else {
                    UIPasteboard.generalPasteboard.string = email
                }
            }
        } else {
            UIApplication.sharedApplication.openURL(it)
        }
    }
}

private const val AUTORUN_TASK_NAME = "org.ooni.probe.autorun-task"

fun registerTaskHandlers(dependencies: Dependencies) {
    val getAutoRunSpecification by lazy { dependencies.getAutoRunSpecification }
    val runDescriptors by lazy { dependencies.runDescriptors }
    val getCurrentTestState by lazy { dependencies.getCurrentTestState }
    val getAutoRunSettings by lazy { dependencies.getAutoRunSettings }

    Logger.d { "Registering task handlers" }
    BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(AUTORUN_TASK_NAME, null) { task ->
        Logger.d { "Received task: $task" }
        (task as? BGProcessingTask)?.let {
            GlobalScope.launch {
                // Sccedule the next autorun task
                configureAutoRun(getAutoRunSettings().first())
            }
            handleAutorunTask(
                task = it,
                getAutoRunSpecification = getAutoRunSpecification,
                runDescriptors = runDescriptors,
                getCurrentTestState = getCurrentTestState,
            )
        }
    }
}

fun configureAutoRun(params: AutoRunParameters) {
    if (params !is AutoRunParameters.Enabled) {
        Logger.d { "Cancelling autorun" }
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(AUTORUN_TASK_NAME)
        return
    }

    Logger.d { "Configuring autorun" }
    scheduleAutorun(params)
}

fun scheduleAutorun(params: AutoRunParameters.Enabled? = null) {
    val error = interpretCPointer<ObjCObjectVar<NSError?>>(alloc(1, 1).rawPtr)
    BGTaskScheduler.sharedScheduler.submitTaskRequest(
        taskRequest = BGProcessingTaskRequest(AUTORUN_TASK_NAME).apply {
            earliestBeginDate = NSDate().dateByAddingTimeInterval(60.0 * 60.0)
            params?.wifiOnly?.let { requiresNetworkConnectivity = it }
            params?.onlyWhileCharging?.let { requiresExternalPower = it }
        },
        error = error,
    )
}

fun handleAutorunTask(
    task: BGProcessingTask,
    getAutoRunSpecification: GetAutoRunSpecification,
    runDescriptors: RunDescriptors,
    getCurrentTestState: () -> Flow<TestRunState>,
) {
    Logger.d { "Handling autorun task" }
    val operationQueue = NSOperationQueue()
    val operation = RunOperation(
        getAutoRunSpecification = getAutoRunSpecification,
        runDescriptors = runDescriptors,
        getCurrentTestState = getCurrentTestState,
    )

    // Provide an expiration handler for the background task that cancels the operation
    task.expirationHandler = {
        operation.cancel()
    }

    // Inform the system that the background task is complete when the operation completes
    operation.completionBlock = {
        task.setTaskCompletedWithSuccess(!operation.isCancelled())
    }

    // Start the operation
    operationQueue.addOperation(operation)
}

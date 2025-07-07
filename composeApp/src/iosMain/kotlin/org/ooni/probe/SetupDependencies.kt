package org.ooni.probe

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.LayoutDirection
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.OonimkallBridge
import org.ooni.probe.background.BackgroundRunner
import org.ooni.probe.background.OperationsManager
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.config.FlavorConfig
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.data.models.DeepLink
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.di.Dependencies
import org.ooni.probe.domain.PreferenceMigration
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import platform.BackgroundTasks.BGProcessingTask
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleLanguageDirectionRightToLeft
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.characterDirectionForLanguage
import platform.Foundation.dateByAddingTimeInterval
import platform.Foundation.stringWithContentsOfFile
import platform.MessageUI.MFMailComposeResult
import platform.MessageUI.MFMailComposeViewController
import platform.MessageUI.MFMailComposeViewControllerDelegateProtocol
import platform.UIKit.UIActivityTypeAirDrop
import platform.UIKit.UIActivityTypePostToFacebook
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState
import platform.UIKit.UIModalPresentationOverCurrentContext
import platform.UIKit.UIPasteboard
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIViewController
import platform.UIKit.UI_USER_INTERFACE_IDIOM
import platform.UIKit.popoverPresentationController
import platform.darwin.NSObject
import platform.darwin.NSObjectMeta
import kotlin.to

class SetupDependencies(
    bridge: OonimkallBridge,
    networkTypeFinder: NetworkTypeFinder,
    val backgroundRunner: BackgroundRunner,
) {
    /**
     * See link for `baseFileDir` https://github.com/ooni/probe-ios/blob/2145bbd5eda6e696be216e3bce97e8d5fb33dcea/ooniprobe/Engine/Engine.m#L54
     * See link for `cacheDir` https://github.com/ooni/probe-ios/blob/2145bbd5eda6e696be216e3bce97e8d5fb33dcea/ooniprobe/Engine/Engine.m#L66
     */
    val dependencies: Dependencies = Dependencies(
        platformInfo = buildPlatformInfo(),
        oonimkallBridge = bridge,
        baseFileDir = baseFileDir(),
        cacheDir = NSTemporaryDirectory(),
        readAssetFile = ::readAssetFile,
        databaseDriverFactory = ::buildDatabaseDriver,
        networkTypeFinder = networkTypeFinder,
        buildDataStore = ::buildDataStore,
        getBatteryState = ::getBatteryState,
        startSingleRunInner = ::startSingleRun,
        configureAutoRun = ::configureAutoRun,
        configureDescriptorAutoUpdate = ::configureDescriptorAutoUpdate,
        cancelDescriptorAutoUpdate = ::cancelDescriptorAutoUpdate,
        startDescriptorsUpdate = ::startDescriptorsUpdate,
        localeDirection = ::localeDirection,
        launchAction = ::launchAction,
        batteryOptimization = object : BatteryOptimization {},
        isWebViewAvailable = { true },
        flavorConfig = FlavorConfig(),
    )

    private val operationsManager = OperationsManager(dependencies, backgroundRunner)

    private var mailDelegate: MFMailComposeViewControllerDelegateProtocol? = null

    private fun localeDirection(): LayoutDirection {
        return if (NSLocale.characterDirectionForLanguage(Locale.current.language) == NSLocaleLanguageDirectionRightToLeft) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
    }

    fun startSingleRun(spec: RunSpecification) {
        operationsManager.startSingleRun(spec)
    }

    fun initializeDeeplink() = MutableSharedFlow<DeepLink>(extraBufferCapacity = 1)

    fun ooniRunDomain() = OrganizationConfig.ooniRunDomain

    private fun buildPlatformInfo() =
        PlatformInfo(
            buildName = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String
                ?: "",
            buildNumber = NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String
                ?: "",
            platform = Platform.Ios,
            osVersion = with(UIDevice.currentDevice) { systemVersion },
            model = UIDevice.currentDevice.model,
            requestNotificationsPermission = false,
            supportsInAppLanguage = true,
            canPullToRefresh = true,
            sentryDsn = "https://a19b2c03b50acdad7d5635559a8e2cad@o155150.ingest.sentry.io/4508325650235392",
        )

    private fun buildDatabaseDriver() = NativeSqliteDriver(schema = Database.Schema, name = "OONIProbe.db")

    /**
     * New asset files need to be added to the iOS project using xCode:
     * - Right click iosApp where you want it and select "Add Files to..."
     * - Pick `src/commonMain/resources`
     * - Deselect "Copy items if needed" and select "Create groups"
     * - Pick both targets OONIProbe and NewsMediaScan
     */
    private fun readAssetFile(path: String): String {
        val fileName = path.split(".").first()
        val type = path.split(".").last()
        val resource = NSBundle.bundleForClass(BundleMarker).pathForResource(fileName, type)
        return resource?.let {
            NSString.stringWithContentsOfFile(
                resource,
                NSUTF8StringEncoding,
                null,
            )
        }
            ?: error("Couldn't read asset file: $path")
    }

    private class BundleMarker : NSObject() {
        companion object : NSObjectMeta()
    }

    private fun getBatteryState(): BatteryState {
        UIDevice.currentDevice.batteryMonitoringEnabled = true
        val isCharging =
            UIDevice.currentDevice.batteryState == UIDeviceBatteryState.UIDeviceBatteryStateCharging
        return if (isCharging) BatteryState.Charging else BatteryState.NotCharging
    }

    fun buildDataStore(): DataStore<Preferences> =
        Dependencies.getDataStore(
            producePath = {
                filesDir() + "/${Dependencies.Companion.DATA_STORE_FILE_NAME}"
            },
            migrations = listOf(PreferenceMigration),
        )

    private fun launchAction(action: PlatformAction): Boolean {
        return when (action) {
            is PlatformAction.Mail -> sendMail(action)
            is PlatformAction.OpenUrl -> openUrl(action)
            is PlatformAction.Share -> shareText(action)
            is PlatformAction.FileSharing -> shareFile(action)
            is PlatformAction.VpnSettings -> openVpnSettings()
            is PlatformAction.LanguageSettings -> openLanguageSettings()
        }
    }

    private fun sendMail(action: PlatformAction.Mail): Boolean {
        if (MFMailComposeViewController.canSendMail()) {
            val delegate = object :
                NSObject(),
                MFMailComposeViewControllerDelegateProtocol {
                override fun mailComposeController(
                    controller: MFMailComposeViewController,
                    didFinishWithResult: MFMailComposeResult,
                    error: NSError?,
                ) {
                    controller.dismissViewControllerAnimated(true, null)
                    mailDelegate = null // Release after use
                }
            }
            mailDelegate = delegate
            MFMailComposeViewController().apply {
                mailComposeDelegate = delegate
                setToRecipients(listOf(action.to))
                setSubject(action.subject)
                setMessageBody(action.body, isHTML = false)
                action.attachment?.let { attachment ->
                    val filePath = filesDir() + "/" + attachment.toString()
                    val fileManager = NSFileManager.defaultManager
                    if (fileManager.fileExistsAtPath(filePath)) {
                        fileManager.contentsAtPath(filePath)?.let { data ->
                            addAttachmentData(
                                attachment = data,
                                mimeType = "application/text",
                                fileName = attachment.name,
                            )
                        }
                    }
                }
            }.let { mailComposer ->
                presentViewController(mailComposer)
            }
            return true
        } else {
            UIPasteboard.generalPasteboard.string = action.to
            return false
        }
    }

    private fun presentViewController(uiViewController: UIViewController): Boolean {
        return findCurrentViewController()?.let { viewController ->

            if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
                uiViewController.popoverPresentationController?.sourceView = viewController.view

                uiViewController.modalPresentationStyle = UIModalPresentationOverCurrentContext
                uiViewController.popoverPresentationController?.sourceRect =
                    CGRectMake(0.0, 0.0, 200.0, 200.0)
            }

            viewController.presentViewController(
                uiViewController,
                true,
                null,
            )
            true
        } ?: run {
            Logger.e { "Cannot find current view controller" }
            false
        }
    }

    fun registerTaskHandlers() {
        Logger.d { "Registering task handlers" }
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            OrganizationConfig.autorunTaskId,
            null,
        ) { task ->
            Logger.d { "Received task: $task" }
            (task as? BGProcessingTask)?.let {
                scheduleNextAutorun()
                operationsManager.handleAutorunTask(it)
            }
        }
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            OrganizationConfig.updateDescriptorTaskId,
            null,
        ) { task ->
            Logger.d { "Received task: $task" }
            (task as? BGProcessingTask)?.let {
                configureDescriptorAutoUpdate()
                operationsManager.handleUpdateDescriptorTask(it)
            }
        }
    }

    fun scheduleNextAutorun() {
        val getAutoRunSettings by lazy { dependencies.getAutoRunSettings }
        CoroutineScope(Dispatchers.Default).launch {
            configureAutoRun(getAutoRunSettings().first())
        }
    }

    fun configureAutoRun(params: AutoRunParameters) {
        if (params !is AutoRunParameters.Enabled) {
            BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(OrganizationConfig.autorunTaskId)
            return
        }
        Logger.d { "Configuring autorun" }
        scheduleAutorun(params)
    }

    fun scheduleAutorun(params: AutoRunParameters.Enabled? = null): Boolean {
        return BGTaskScheduler.sharedScheduler.submitTaskRequest(
            taskRequest = BGProcessingTaskRequest(OrganizationConfig.autorunTaskId).apply {
                earliestBeginDate = NSDate().dateByAddingTimeInterval(60.0 * 60.0)
                requiresNetworkConnectivity = true
                params?.onlyWhileCharging?.let { requiresExternalPower = it }
            },
            error = null,
        )
    }

    private fun openVpnSettings() = openInternalUrl("App-prefs:General&path=ManagedConfigurationList")

    private fun openLanguageSettings() = openInternalUrl(UIApplicationOpenSettingsURLString)

    private fun openInternalUrl(urlString: String): Boolean =
        NSURL.URLWithString(urlString)?.let { nsUrlString ->
            if (UIApplication.sharedApplication.canOpenURL(nsUrlString)) {
                UIApplication.sharedApplication.openURL(url = nsUrlString, options = emptyMap<Any?, Any>(), completionHandler = {
                        isSuccessfullyOpened ->
                    if (isSuccessfullyOpened) {
                        Logger.i { "Successfully opened URL: $urlString" }
                    } else {
                        Logger.e { "Failed to open URL: $urlString" }
                    }
                })
                return@let true
            } else {
                Logger.e { "Cannot open URL: $urlString" }
                return@let false
            }
        } ?: false

    private fun cancelDescriptorAutoUpdate(): Boolean {
        Logger.d("Cancelling descriptor auto update")
        return try {
            BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(OrganizationConfig.updateDescriptorTaskId)
            true
        } catch (e: Exception) {
            Logger.e(e) { "Failed to cancel descriptor auto update" }
            false
        }
    }

    private fun configureDescriptorAutoUpdate(): Boolean {
        Logger.d("Configuring descriptor auto update")
        return BGTaskScheduler.sharedScheduler.submitTaskRequest(
            BGProcessingTaskRequest(OrganizationConfig.updateDescriptorTaskId).apply {
                requiresNetworkConnectivity = true
                earliestBeginDate = NSDate().dateByAddingTimeInterval(60.0 * 60.0 * 24.0)
            },
            error = null,
        )
    }

    fun startDescriptorsUpdate(descriptors: List<InstalledTestDescriptorModel>?) {
        operationsManager.startDescriptorsUpdate(descriptors)
    }

    private fun shareText(share: PlatformAction.Share): Boolean {
        val activityViewController = UIActivityViewController(
            activityItems = listOf(share.text),
            applicationActivities = null,
        )

        return presentViewController(activityViewController)
    }

    private fun shareFile(share: PlatformAction.FileSharing): Boolean {
        val filePath = filesDir() + "/" + share.filePath.toString()

        val url = NSURL.fileURLWithPath(filePath)
        val activityViewController = UIActivityViewController(
            activityItems = listOf(url),
            applicationActivities = null,
        )
        activityViewController.excludedActivityTypes =
            listOf(UIActivityTypeAirDrop, UIActivityTypePostToFacebook)

        return presentViewController(activityViewController)
    }

    private fun openUrl(openUrl: PlatformAction.OpenUrl): Boolean {
        val urlString = openUrl.url
        return NSURL.URLWithString(urlString)?.let { nsUrlString ->
            if (UIApplication.sharedApplication.canOpenURL(nsUrlString)) {
                UIApplication.sharedApplication.openURL(url = nsUrlString, options = emptyMap<Any?, Any>(), completionHandler = {
                        isSuccessfullyOpened ->
                    if (isSuccessfullyOpened) {
                        Logger.i { "Successfully opened URL: $urlString" }
                    } else {
                        Logger.e { "Failed to open URL: $urlString" }
                    }
                })
                return@let true
            } else {
                Logger.e { "Cannot open URL: $urlString" }
                return@let false
            }
        } ?: false
    }

    private fun baseFileDir(): String {
        return NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true,
        ).first().toString()
    }

    private fun filesDir(): String? {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )

        return requireNotNull(documentDirectory).path
    }

    private fun findCurrentViewController(): UIViewController? {
        return UIApplication.sharedApplication.keyWindow?.rootViewController
    }
}

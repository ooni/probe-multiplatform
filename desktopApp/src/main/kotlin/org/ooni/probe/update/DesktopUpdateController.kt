package org.ooni.probe.update

import androidx.compose.ui.window.ApplicationScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withTimeoutOrNull
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Tray_Notification_Check_Updates
import ooniprobe.composeapp.generated.resources.Tray_Notification_Checking_Updates
import ooniprobe.composeapp.generated.resources.Tray_Notification_Updates_Available
import org.jetbrains.compose.resources.StringResource
import org.ooni.probe.config.UpdateConfig
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.dependencies
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.Distribution
import org.ooni.probe.shared.NoOpUpdateManager
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.SparkleUpdateManager
import org.ooni.probe.shared.UpdateError
import org.ooni.probe.shared.UpdateManager
import org.ooni.probe.shared.UpdateState
import org.ooni.probe.shared.WinSparkleUpdateManager
import kotlin.time.Duration.Companion.minutes

/**
 * DesktopUpdateController encapsulates update logic for desktop targets.
 * It is the only owner of the [UpdateManager]: it exposes state and error as flows,
 * provides actions, and runs single feed checks for [UpdateFeedFailover].
 *
 * On networks where a feed host is throttled or unreachable, failed checks are retried
 * with backoff and then failed over to the next feed URL in [UpdateConfig.URLS].
 */
class DesktopUpdateController(
    private val updateManager: UpdateManager,
    private val preferenceRepository: PreferenceRepository = dependencies.preferenceRepository,
) {
    private val _state = MutableStateFlow(UpdateState.IDLE)
    val state: StateFlow<UpdateState> = _state

    private val _error = MutableStateFlow<UpdateError?>(null)
    val error: StateFlow<UpdateError?> = _error

    private val urls: List<String> get() = UpdateConfig.URLS

    @Volatile
    private var failover: UpdateFeedFailover? = null

    // Feed checks run one at a time on a single-threaded dispatcher
    @Volatile
    private var checkScope: CoroutineScope? = null
    private var checkJob: Job? = null

    private var activeUrl: String? = null

    @Volatile
    private var pendingOutcome: CompletableDeferred<CheckOutcome>? = null

    /**
     * Initialize update manager and set callbacks. Safe to call multiple times.
     */
    fun initialize(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            try {
                if (updateManager is WinSparkleUpdateManager) {
                    updateManager.setDllRoot(System.getProperty("compose.application.resources.dir") ?: "")
                }

                if (urls.isEmpty()) {
                    Logger.w("No appcast feed URLs configured for this platform, updates disabled")
                    return@launch
                }

                val failover = UpdateFeedFailover(
                    urls = urls,
                    check = ::runCheck,
                    loadSavedIndex = {
                        preferenceRepository.getValueByKey(SettingsKey.LAST_WORKING_UPDATE_FEED_INDEX)
                            .firstOrNull() as? Int
                    },
                    saveIndex = { index ->
                        preferenceRepository.setValueByKey(SettingsKey.LAST_WORKING_UPDATE_FEED_INDEX, index)
                    },
                )
                val initialUrl = failover.loadPreferredUrl()
                activeUrl = initialUrl

                updateManager.setErrorCallback { error ->
                    Logger.e("Update ${error.operation} failed with ${error.code} (${CheckOutcome.describe(error)}): ${error.message}")
                    _error.value = error
                    _state.value = updateManager.getCurrentState()
                    pendingOutcome?.complete(CheckOutcome.fromError(error))
                }

                updateManager.setStateCallback { s ->
                    Logger.d("Update system state: $s")
                    _state.value = s
                    _error.value = updateManager.getLastError()

                    when (s) {
                        UpdateState.UPDATE_AVAILABLE -> pendingOutcome?.complete(CheckOutcome.UpdateAvailable)
                        UpdateState.NO_UPDATE_AVAILABLE -> pendingOutcome?.complete(CheckOutcome.NoUpdate)
                        else -> Unit
                    }
                }

                updateManager.setLogCallback { message ->
                    Logger.d("Update system ${message.operation}: ${message.message}")
                }

                Logger.i("Initializing update system...")
                Logger.i("Appcast feeds: ${urls.joinToString()} (starting with $initialUrl)")
                Logger.i("Public key configured: ${UpdateConfig.PUBLIC_KEY.isNotEmpty()}")

                updateManager.initialize(initialUrl, UpdateConfig.PUBLIC_KEY)
                updateManager.setAutomaticUpdatesEnabled(true)
                updateManager.setUpdateCheckInterval(UpdateConfig.CHECK_INTERVAL_HOURS)

                checkScope = scope + Dispatchers.Default.limitedParallelism(1)
                this@DesktopUpdateController.failover = failover
                checkOnForeground()
            } catch (e: Throwable) {
                Logger.e("Failed to initialize update system: $e")
                _error.value = UpdateError(-999, "Setup failed: ${e.message}", "setup")
                _state.value = UpdateState.ERROR
            }
        }
    }

    fun registerShutdownHandler(appScope: ApplicationScope) {
        try {
            when (updateManager) {
                is WinSparkleUpdateManager -> updateManager.setShutdownCallback {
                    Logger.i("WinSparkle requested application shutdown for update installation")
                    appScope.exitApplication()
                }
                is SparkleUpdateManager -> updateManager.setShutdownCallback {
                    Logger.i("Sparkle requested application shutdown for update installation")
                    appScope.exitApplication()
                }
            }
        } catch (e: Throwable) {
            Logger.w("Failed to register update shutdown handler, updates disabled", e)
        }
    }

    fun checkNow() {
        val failover = failover ?: return
        if (!updateManager.isHealthy()) {
            // Checks are refused in this state, so retry the operation that failed instead
            Logger.i("Retrying update operation after error")
            updateManager.retryLastOperation()
            return
        }
        // A user check replaces any check in progress, including one waiting to retry
        startCheck(replaceRunning = true) {
            val canDeferErrors = updateManager.canDeferErrorDialogs
            if (canDeferErrors) updateManager.setDeferErrorDialogs(true)
            try {
                val outcome = failover.checkByUser(canDeferErrors)
                if (canDeferErrors && !outcome.feedWorked) {
                    updateManager.showDeferredErrorDialog()
                }
            } finally {
                if (canDeferErrors) updateManager.setDeferErrorDialogs(false)
                useFeed(failover.currentUrl)
            }
        }
    }

    fun retryLastOperation() {
        updateManager.retryLastOperation()
    }

    /**
     * Silent update check for automatic triggers (app launch, window foregrounded).
     * Unlike [checkNow], this never shows native update UI/dialogs.
     */
    fun checkOnForeground() {
        if (!supportsUpdates()) return
        if (_state.value == UpdateState.CHECKING_FOR_UPDATES) return
        val failover = failover ?: return
        // A check in progress (or waiting to retry) already covers this one
        startCheck(replaceRunning = false) {
            try {
                failover.checkInBackground()
            } finally {
                useFeed(failover.currentUrl)
            }
        }
    }

    @Synchronized
    private fun startCheck(
        replaceRunning: Boolean,
        block: suspend () -> Unit,
    ) {
        val scope = checkScope ?: return
        val running = checkJob
        if (running?.isActive == true) {
            if (!replaceRunning) return
            running.cancel()
        }
        checkJob = scope.launch {
            running?.join()
            block()
        }
    }

    /**
     * Runs one check against [url] and waits for the updater to report how it ended.
     */
    private suspend fun runCheck(
        url: String,
        showUI: Boolean,
    ): CheckOutcome {
        if (!updateManager.isHealthy()) {
            Logger.w("Update system unhealthy, skipping check of $url")
            return CheckOutcome.OtherError
        }
        useFeed(url)
        val outcome = CompletableDeferred<CheckOutcome>()
        pendingOutcome = outcome
        try {
            updateManager.checkForUpdates(showUI)
            return withTimeoutOrNull(CHECK_TIMEOUT) { outcome.await() }
                ?: CheckOutcome.FeedFailure.also { Logger.w("Update check of $url timed out") }
        } finally {
            if (pendingOutcome === outcome) pendingOutcome = null
        }
    }

    private fun useFeed(url: String) {
        if (url == activeUrl) return
        activeUrl = url
        updateManager.setAppcastUrl(url)
    }

    fun getMenuText(): StringResource =
        when (_state.value) {
            UpdateState.CHECKING_FOR_UPDATES -> Res.string.Tray_Notification_Checking_Updates
            UpdateState.UPDATE_AVAILABLE -> Res.string.Tray_Notification_Updates_Available
            else -> Res.string.Tray_Notification_Check_Updates
        }

    suspend fun cleanup() {
        try {
            updateManager.cleanup()
            Logger.i("Update manager cleaned up successfully")
        } catch (e: Exception) {
            Logger.e("Error during update manager cleanup: $e")
        }
    }

    fun supportsUpdates(): Boolean =
        Distribution.current.supportsSelfUpdate &&
            updateManager !is NoOpUpdateManager &&
            (dependencies.platformInfo.platform as? Platform.Desktop)?.os in listOf(DesktopOS.Mac, DesktopOS.Windows) &&
            updateManager.isHealthy()

    companion object {
        // Upper bound for when the native updater never reports back (e.g. a swallowed check)
        private val CHECK_TIMEOUT = 2.minutes
    }
}

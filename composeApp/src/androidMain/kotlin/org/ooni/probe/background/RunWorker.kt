package org.ooni.probe.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.collectLatest
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Running
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Notice
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Title
import ooniprobe.composeapp.generated.resources.Notification_ChannelName
import ooniprobe.composeapp.generated.resources.Notification_StopTest
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Results_UploadingMissing
import org.jetbrains.compose.resources.getString
import org.ooni.probe.AndroidApplication
import org.ooni.probe.MainActivity
import org.ooni.probe.R
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.di.Dependencies
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.ui.primaryLight
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

class RunWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val dependencies by lazy { (appContext as AndroidApplication).dependencies }
    private val json by lazy { dependencies.json }
    private val runBackgroundTask by lazy { dependencies.runBackgroundTask }
    private val cancelCurrentTest by lazy { dependencies.cancelCurrentTest }
    private val setRunBackgroundState by lazy {
        dependencies.runBackgroundStateManager::updateState
    }

    private val notificationManager by lazy {
        appContext.getSystemService(NotificationManager::class.java)
    }

    private val stopRunReceiver by lazy { StopRunReceiver() }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        buildNotificationChannelIfNeeded()
        val notification = buildNotification(RunBackgroundState.RunningTests())
        return buildForegroundInfo(notification)
    }

    override suspend fun doWork(): Result {
        Logger.i("Run Worker: start")
        try {
            setForeground(getForegroundInfo())
        } catch (e: IllegalStateException) {
            e.message?.let(Logger::i)
            Logger.w(
                "Run Worker: cannot start due to foreground service restriction",
                ForegroundServiceRestriction(),
            )
            return Result.failure()
        }

        try {
            registerReceiver()
            work()
        } catch (e: CancellationException) {
            if (isStopped) {
                Logger.w(
                    "Run Worker: early stop",
                    EarlyStop(if (Build.VERSION.SDK_INT >= 31) stopReason else null),
                )
            } else {
                Logger.i("Run Worker: cancelled")
            }
            setRunBackgroundState { RunBackgroundState.Idle() }
        } finally {
            notificationManager.cancel(NOTIFICATION_ID)
            unregisterReceiver()
            Logger.i("Run Worker: finished")
        }
        return Result.success()
    }

    private suspend fun work() {
        try {
            getSpecification()
        } catch (e: Exception) {
            Logger.w("Could not start RunService: invalid spec", e)
            return
        }

        runBackgroundTask(getSpecification())
            .collectLatest { state ->
                val notification = when (state) {
                    is RunBackgroundState.Idle -> null
                    is RunBackgroundState.UploadingMissingResults -> buildNotification(state.state)
                    is RunBackgroundState.RunningTests -> buildNotification(state)
                    is RunBackgroundState.Stopping -> buildStoppingNotification()
                }
                if (notification != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
    }

    private fun registerReceiver() {
        ContextCompat.registerReceiver(
            applicationContext,
            stopRunReceiver,
            IntentFilter(ACTION_STOP_RUN),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private fun unregisterReceiver() {
        applicationContext.unregisterReceiver(stopRunReceiver)
    }

    private fun getSpecification(): RunSpecification? {
        val specJson = inputData.getString(DATA_KEY_SPEC)
        return if (specJson != null) {
            json.decodeFromString<RunSpecification>(specJson)
        } else {
            null
        }
    }

    private suspend fun buildNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(Res.string.Notification_ChannelName),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private fun buildForegroundInfo(notification: Notification): ForegroundInfo =
        if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
            )
        }

    private suspend fun buildNotification(state: UploadMissingMeasurements.State) =
        buildNotification {
            if (state is UploadMissingMeasurements.State.Uploading) {
                val progress = state.uploaded + state.failedToUpload + 1
                setContentText(
                    getString(
                        Res.string.Results_UploadingMissing,
                        state.progressText,
                    ),
                ).setProgress(state.total, progress, false)
                    .addAction(buildNotificationStopAction())
            } else {
                setProgress(1, 0, true)
            }
        }

    private suspend fun buildNotification(state: RunBackgroundState.RunningTests) =
        buildNotification {
            setContentText(state.testType?.displayNameSuspended())
                .setColor(state.descriptor?.color?.toArgb() ?: primaryLight.toArgb())
                .setProgress(1000, (state.progress * 1000).roundToInt(), false)
                .addAction(buildNotificationStopAction())
        }

    private suspend fun buildStoppingNotification() =
        buildNotification {
            setContentTitle(getString(Res.string.Dashboard_Running_Stopping_Title))
                .setContentText(getString(Res.string.Dashboard_Running_Stopping_Notice))
                .setProgress(1, 0, true)
        }

    private suspend fun buildNotification(build: suspend NotificationCompat.Builder.() -> NotificationCompat.Builder): Notification =
        build(
            NotificationCompat
                .Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getString(Res.string.Dashboard_Running_Running))
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSound(null)
                .setVibrate(null)
                .setOnlyAlertOnce(true)
                .setLights(0, 0, 0)
                .setColor(primaryLight.toArgb())
                .setContentIntent(openAppIntent),
        ).build()

    private suspend fun buildNotificationStopAction() =
        NotificationCompat.Action
            .Builder(
                null,
                getString(Res.string.Notification_StopTest),
                stopRunIntent,
            ).build()

    private val openAppIntent
        get() = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private val stopRunIntent
        get() = PendingIntent.getBroadcast(
            applicationContext,
            1,
            Intent(ACTION_STOP_RUN),
            PendingIntent.FLAG_IMMUTABLE,
        )

    private inner class StopRunReceiver : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent,
        ) {
            if (intent.action != ACTION_STOP_RUN) return
            cancelCurrentTest()
        }
    }

    class ForegroundServiceRestriction : Exception()

    class EarlyStop(
        reason: Int?,
    ) : EarlyStopWorkerException(reason)

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "RUN"
        private const val NOTIFICATION_ID = 1
        private const val DATA_KEY_SPEC = "spec"
        private const val ACTION_STOP_RUN = "stop_run"

        fun buildWorkData(spec: RunSpecification): Data {
            val specWithoutInstalledInputs = spec.stripInstalledInputs()
            val specJson = Dependencies.buildJson().encodeToString(specWithoutInstalledInputs)
            return workDataOf(DATA_KEY_SPEC to specJson)
        }
    }
}

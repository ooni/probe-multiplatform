package org.ooni.probe.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.serialization.encodeToString
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Running
import ooniprobe.composeapp.generated.resources.Notification_StopTest
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.notification_channel_name
import org.jetbrains.compose.resources.getString
import org.ooni.probe.AndroidApplication
import org.ooni.probe.MainActivity
import org.ooni.probe.R
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.di.Dependencies
import org.ooni.probe.ui.primaryLight
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

class RunWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val dependencies by lazy { (appContext as AndroidApplication).dependencies }
    private val json by lazy { dependencies.json }
    private val getAutoRunSpecification by lazy { dependencies.getAutoRunSpecification }
    private val runDescriptors by lazy { dependencies.runDescriptors }
    private val getCurrentTestState by lazy { dependencies.getCurrentTestState }
    private val cancelCurrentTest by lazy { dependencies.cancelCurrentTest }

    private val notificationManager by lazy {
        appContext.getSystemService(NotificationManager::class.java)
    }

    private val stopRunReceiver by lazy { StopRunReceiver() }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        buildNotificationChannelIfNeeded()
        return ForegroundInfo(NOTIFICATION_ID, buildNotification(TestRunState.Running()))
    }

    override suspend fun doWork(): Result {
        try {
            Logger.i("Run Worker: started")
            registerReceiver()
            buildNotificationChannelIfNeeded()

            work()
        } catch (e: CancellationException) {
            Logger.i("Run Worker: cancelled")
        } finally {
            notificationManager.cancel(NOTIFICATION_ID)
            unregisterReceiver()
            Logger.i("Run Worker: finished")
        }
        return Result.success()
    }

    private suspend fun work() {
        val spec = getSpecification() ?: return

        // Start the actual run
        coroutineScope {
            val runJob = async {
                runDescriptors(spec)
            }

            // Observe the run state to update the notifications and finish the worker when it's done
            var testStarted = false
            getCurrentTestState()
                .takeWhile { state ->
                    state is TestRunState.Running || (state is TestRunState.Idle && !testStarted)
                }
                .onEach { state ->
                    if (state !is TestRunState.Running) return@onEach
                    testStarted = true
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
                }
                .collect()

            runJob.await()
        }
    }

    private fun registerReceiver() {
        ContextCompat.registerReceiver(
            applicationContext,
            stopRunReceiver,
            IntentFilter(ACTION_STOP_RUN),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun unregisterReceiver() {
        applicationContext.unregisterReceiver(stopRunReceiver)
    }

    private suspend fun getSpecification(): RunSpecification? {
        val specJson = inputData.getString(DATA_KEY_SPEC)
        if (specJson != null) {
            try {
                return json.decodeFromString<RunSpecification>(specJson)
            } catch (e: Exception) {
                Logger.w("Could not start RunService: invalid spec", e)
                return null
            }
        }

        return getAutoRunSpecification()
    }

    private suspend fun buildNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(Res.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }

    private suspend fun buildNotification(state: TestRunState.Running): Notification {
        return NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(Res.string.Dashboard_Running_Running))
            .setContentText(state.testType?.labelRes?.let { getString(it) })
            .setColor(state.descriptor?.color?.toArgb() ?: primaryLight.toArgb())
            .setProgress(1000, (state.progress * 1000).roundToInt(), false)
            .setAutoCancel(false)
            .setContentIntent(openAppIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    getString(Res.string.Notification_StopTest),
                    stopRunIntent,
                ).build(),
            )
            .build()
    }

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

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "RUN"
        private const val NOTIFICATION_ID = 1
        private const val DATA_KEY_SPEC = "spec"
        private const val ACTION_STOP_RUN = "stop_run"

        fun buildWorkData(spec: RunSpecification) = workDataOf(DATA_KEY_SPEC to Dependencies.buildJson().encodeToString(spec))
    }
}

package org.ooni.probe.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Running
import ooniprobe.composeapp.generated.resources.Notification_StopTest
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.notification_channel_name
import org.jetbrains.compose.resources.StringResource
import org.ooni.probe.AndroidApplication
import org.ooni.probe.MainActivity
import org.ooni.probe.R
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.di.Dependencies
import org.ooni.probe.ui.primaryLight
import kotlin.math.roundToInt

class RunService : Service() {
    private val dependencies by lazy { (application as AndroidApplication).dependencies }
    private val backgroundDispatcher by lazy { dependencies.backgroundDispatcher }
    private val json by lazy { dependencies.json }
    private val runDescriptors by lazy { dependencies.runDescriptors }
    private val getCurrentTestState by lazy { dependencies.getCurrentTestState }
    private val cancelCurrentTest by lazy { dependencies.cancelCurrentTest }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    private val stopRunReceiver by lazy { StopRunReceiver() }

    override fun onCreate() {
        super.onCreate()
        val intentFilter = IntentFilter(ACTION_STOP_RUN)
        ContextCompat.registerReceiver(
            this,
            stopRunReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(stopRunReceiver)
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val specJson = intent?.getStringExtra(EXTRA_SPEC) ?: run {
            Logger.w("Could not start RunService: spec missing")
            return START_NOT_STICKY
        }

        val spec = try {
            json.decodeFromString<RunSpecification>(specJson)
        } catch (e: Exception) {
            Logger.w("Could not start RunService: invalid spec", e)
            return START_NOT_STICKY
        }

        runBlocking {
            if (!startForeground()) {
                stopSelf()
                return@runBlocking
            }

            // Start the actual run
            CoroutineScope(backgroundDispatcher).launch {
                runDescriptors(spec)
            }

            // Observe the run state to update the notifications and stop the service
            var testStarted = false
            getCurrentTestState()
                .onEach { testState ->
                    when (testState) {
                        is TestRunState.Idle -> {
                            if (testStarted) stopSelf()
                        }

                        is TestRunState.Running -> {
                            testStarted = true
                            notificationManager.notify(
                                NOTIFICATION_ID,
                                buildNotification(testState),
                            )
                        }

                        TestRunState.Stopping -> {
                            stopSelf()
                        }
                    }
                }
                .launchIn(CoroutineScope(backgroundDispatcher))
        }

        return START_NOT_STICKY
    }

    private suspend fun startForeground(): Boolean {
        return try {
            buildNotificationChannelIfNeeded()
            val notification = buildNotification(TestRunState.Running())

            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                },
            )
            true
        } catch (e: Exception) {
            Logger.w("Could not start foreground RunService", e)
            false
        }
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
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(Res.string.Dashboard_Running_Running))
            .setContentText(state.testType?.labelRes?.let { getString(it) })
            .setColor(state.descriptor?.color?.toArgb() ?: primaryLight.toArgb())
            .setProgress(1000, (state.testProgress * 1000).roundToInt(), false)
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
            this,
            0,
            Intent(this, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )

    private val stopRunIntent
        get() = PendingIntent.getBroadcast(
            this,
            1,
            Intent(ACTION_STOP_RUN),
            PendingIntent.FLAG_IMMUTABLE,
        )

    private suspend fun getString(stringResource: StringResource) = org.jetbrains.compose.resources.getString(stringResource)

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
        private const val EXTRA_SPEC = "spec"
        private const val ACTION_STOP_RUN = "stop_run"

        fun start(
            context: Context,
            spec: RunSpecification,
        ) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, RunService::class.java)
                    .putExtra(EXTRA_SPEC, Dependencies.buildJson().encodeToString(spec)),
            )
        }
    }
}

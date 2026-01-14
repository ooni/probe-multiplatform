package org.ooni.probe.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import ooniprobe.composeapp.generated.resources.Dashboard_Progress_UpdateLink_Label
import ooniprobe.composeapp.generated.resources.Notification_UpdateChannelName
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.getString
import org.ooni.probe.AndroidApplication
import org.ooni.probe.R
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.di.Dependencies

class DescriptorUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val dependencies by lazy { (appContext as AndroidApplication).dependencies }
    private val json by lazy { dependencies.json }
    private val testDescriptorRepository by lazy { dependencies.testDescriptorRepository }
    private val notificationManager by lazy {
        appContext.getSystemService(NotificationManager::class.java)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        buildNotificationChannelIfNeeded()
        val notification = buildNotification()
        return if (Build.VERSION.SDK_INT >= 29) {
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
    }

    override suspend fun doWork(): Result {
        Logger.i("DescriptorUpdateWorker: start")
        try {
            setForeground(getForegroundInfo())
        } catch (e: IllegalStateException) {
            e.message?.let(Logger::i)
            Logger.w(
                "DescriptorUpdateWorker: cannot start due to foreground service restriction",
                ForegroundServiceRestriction(),
            )
            return Result.failure()
        }

        return try {
            val descriptors = getDescriptors() ?: return Result.failure()
            dependencies.fetchDescriptorsUpdates(descriptors)
            Result.success(buildWorkData(descriptors.map { it.id }))
        } catch (e: CancellationException) {
            if (isStopped) {
                Logger.w(
                    "DescriptorUpdateWorker: early stop",
                    EarlyStop(if (Build.VERSION.SDK_INT >= 31) stopReason else null),
                )
            } else {
                Logger.w("DescriptorUpdateWorker: cancelled", e)
            }
            Result.failure()
        } finally {
            Logger.i("DescriptorUpdateWorker: finished")
        }
    }

    private suspend fun getDescriptors(): List<Descriptor>? {
        val descriptorsJson = inputData.getString(DATA_KEY_DESCRIPTORS)
        if (descriptorsJson != null) {
            try {
                val ids =
                    json.decodeFromString<List<Descriptor.Id>>(descriptorsJson)
                return testDescriptorRepository.listLatestByRunIds(ids).first()
            } catch (e: SerializationException) {
                Logger.w("Could not start update worker: invalid configuration", e)
                return null
            } catch (e: IllegalArgumentException) {
                Logger.w("Could not start update worker: invalid configuration", e)
                return null
            }
        } else {
            return emptyList() // FetchDescriptorsUpdates will update them all
        }
    }

    private suspend fun buildNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(Res.string.Notification_UpdateChannelName),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private suspend fun buildNotification(): Notification =
        NotificationCompat
            .Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(Res.string.Dashboard_Progress_UpdateLink_Label))
            .setAutoCancel(false)
            .build()

    class ForegroundServiceRestriction : Exception()

    class EarlyStop(
        reason: Int?,
    ) : EarlyStopWorkerException(reason)

    companion object {
        fun buildWorkData(descriptors: List<Descriptor.Id>): Data =
            workDataOf(
                DATA_KEY_DESCRIPTORS to Dependencies.buildJson().encodeToString(descriptors),
            )

        private const val NOTIFICATION_CHANNEL_ID = "UPDATES"
        private const val NOTIFICATION_ID = 2
        private const val DATA_KEY_DESCRIPTORS = "descriptors"

        val AutoUpdateWorkerName: String = DescriptorUpdateWorker::class.java.name
        val ManualUpdateWorkerName: String = "$AutoUpdateWorkerName.manual"
    }
}

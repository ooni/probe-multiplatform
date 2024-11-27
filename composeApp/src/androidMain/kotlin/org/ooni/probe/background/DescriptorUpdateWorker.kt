package org.ooni.probe.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import kotlinx.serialization.encodeToString
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Running
import ooniprobe.composeapp.generated.resources.Notification_ChannelName
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.getString
import org.ooni.probe.AndroidApplication
import org.ooni.probe.R
import org.ooni.probe.data.models.InstalledTestDescriptorModel
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
        return ForegroundInfo(NOTIFICATION_ID, buildNotification())
    }

    override suspend fun doWork(): Result {
        try {
            val descriptors = getDescriptors() ?: return Result.failure()
            if (descriptors.isEmpty()) {
                Logger.i("Skipping DescriptorUpdateWorker: no descriptors to update")
                return Result.success(buildWorkData(descriptors.map { it.id }))
            }
            dependencies.getDescriptorUpdate.invoke(descriptors)
            return Result.success(buildWorkData(descriptors.map { it.id }))
        } catch (e: CancellationException) {
            Logger.w("DescriptorUpdateWorker: cancelled", e)
            return Result.failure()
        }
    }

    private suspend fun getDescriptors(): List<InstalledTestDescriptorModel>? {
        val descriptorsJson = inputData.getString(DATA_KEY_DESCRIPTORS)
        if (descriptorsJson != null) {
            try {
                val ids =
                    json.decodeFromString<List<InstalledTestDescriptorModel.Id>>(descriptorsJson)
                return testDescriptorRepository.selectByRunIds(ids).first()
            } catch (e: SerializationException) {
                Logger.w("Could not start update worker: invalid configuration", e)
                return null
            } catch (e: IllegalArgumentException) {
                Logger.w("Could not start update worker: invalid configuration", e)
                return null
            }
        }
        return testDescriptorRepository.list().first()
    }

    private suspend fun buildNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(Res.string.Notification_ChannelName),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }

    private suspend fun buildNotification(): Notification {
        return NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(Res.string.Dashboard_Running_Running))
            .setAutoCancel(false)
            .build()
    }

    companion object {
        fun buildWorkData(descriptors: List<InstalledTestDescriptorModel.Id>): Data {
            return workDataOf(
                DATA_KEY_DESCRIPTORS to Dependencies.buildJson().encodeToString(descriptors),
            )
        }

        private const val NOTIFICATION_CHANNEL_ID = "UPDATES"
        private const val NOTIFICATION_ID = 1
        private const val DATA_KEY_DESCRIPTORS = "descriptors"

        val AutoUpdateWorkerName: String = DescriptorUpdateWorker::class.java.name
        val ManualUpdateWorkerName: String = "$AutoUpdateWorkerName.manual"
    }
}

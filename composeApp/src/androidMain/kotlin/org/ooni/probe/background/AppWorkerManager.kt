package org.ooni.probe.background

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.RunSpecification
import java.util.concurrent.TimeUnit

class AppWorkerManager(
    private val workManager: WorkManager,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    fun startSingleRun(spec: RunSpecification) {
        workManager
            .enqueueUniqueWork(
                RUN_UNIQUE_WORKER_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<RunWorker>()
                    .setInputData(RunWorker.buildWorkData(spec))
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build(),
            )
    }

    suspend fun configureAutoRun(params: AutoRunParameters) {
        withContext(backgroundDispatcher) {
            if (params !is AutoRunParameters.Enabled) {
                workManager.cancelUniqueWork(AUTO_RUN_UNIQUE_WORKER_NAME)
                return@withContext
            }

            val request = PeriodicWorkRequestBuilder<RunWorker>(1, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(
                            if (params.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED,
                        )
                        .setRequiresCharging(params.onlyWhileCharging)
                        .build(),
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                AUTO_RUN_UNIQUE_WORKER_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }

    suspend fun configureDescriptorAutoUpdate(): Boolean {
        return withContext(backgroundDispatcher) {
            val request = PeriodicWorkRequestBuilder<DescriptorUpdateWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS) // avoid immediate start
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            workManager.enqueueUniquePeriodicWork(
                DescriptorUpdateWorker.AutoUpdateWorkerName,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request,
            )
            true
        }
    }

    suspend fun fetchDescriptorUpdate(descriptors: List<InstalledTestDescriptorModel>?) {
        withContext(backgroundDispatcher) {
            workManager
                .enqueueUniqueWork(
                    DescriptorUpdateWorker.ManualUpdateWorkerName,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<DescriptorUpdateWorker>()
                        .setInputData(
                            descriptors?.let {
                                DescriptorUpdateWorker.buildWorkData(
                                    it.map { descriptor -> descriptor.id },
                                )
                            } ?: Data.EMPTY,
                        )
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build(),
                )
        }
    }

    companion object {
        private const val RUN_UNIQUE_WORKER_NAME = "run"
        private const val AUTO_RUN_UNIQUE_WORKER_NAME = "auto_run"
    }
}

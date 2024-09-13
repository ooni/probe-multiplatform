package org.ooni.probe.background

import androidx.work.Constraints
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
import org.ooni.probe.data.models.RunSpecification
import java.util.concurrent.TimeUnit

class RunWorkerManager(
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
                // TODO: Confirm with the team if we want to auto-test with a VPN on
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(
                            if (params.wifiOnly) NetworkType.UNMETERED else NetworkType.NOT_REQUIRED,
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

    companion object {
        private const val RUN_UNIQUE_WORKER_NAME = "run"
        private const val AUTO_RUN_UNIQUE_WORKER_NAME = "auto_run"
    }
}

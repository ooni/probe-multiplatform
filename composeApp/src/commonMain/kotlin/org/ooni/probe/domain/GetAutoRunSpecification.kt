package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.DescriptorItem
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.notExpired
import org.ooni.probe.data.repositories.PreferenceRepository

class GetAutoRunSpecification(
    private val getLatestDescriptors: () -> Flow<List<DescriptorItem>>,
    private val preferenceRepository: PreferenceRepository,
) {
    suspend operator fun invoke(): RunSpecification.Full {
        val descriptors = getLatestDescriptors()
            .first()
            .filterForAutoRun()
            .notExpired()

        return RunSpecification.Full(
            tests = descriptors.mapNotNull { descriptor ->
                val enabledNetTests = descriptor.netTests
                    .filter { netTest -> descriptor.isEnabledForAutoRun(netTest) }
                val enabledLongRunningTests = descriptor.longRunningTests
                    .filter { netTest -> descriptor.isEnabledForAutoRun(netTest) }

                val allEnabledTests = enabledNetTests + enabledLongRunningTests

                if (allEnabledTests.isNotEmpty()) {
                    RunSpecification.Test(
                        descriptorId = descriptor.descriptor.id,
                        netTests = allEnabledTests,
                    )
                } else {
                    null
                }
            },
            taskOrigin = TaskOrigin.AutoRun,
            isRerun = false,
        )
    }

    private fun List<DescriptorItem>.filterForAutoRun() = filter { it.enabled }

    private suspend fun DescriptorItem.isEnabledForAutoRun(netTest: NetTest) =
        preferenceRepository.isNetTestEnabled(this, netTest, isAutoRun = true).first()
}

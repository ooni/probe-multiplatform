package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.notExpired
import org.ooni.probe.data.repositories.PreferenceRepository

class GetAutoRunSpecification(
    private val getLatestDescriptors: () -> Flow<List<Descriptor>>,
    private val preferenceRepository: PreferenceRepository,
) {
    suspend operator fun invoke(): RunSpecification.Full {
        val descriptors = getLatestDescriptors().first().filterForAutoRun().notExpired()

        return RunSpecification.Full(
            tests = descriptors.map { descriptor ->
                RunSpecification.Test(
                    source = RunSpecification.Test.Source.fromDescriptor(descriptor),
                    netTests = descriptor.netTests,
                )
            },
            taskOrigin = TaskOrigin.AutoRun,
            isRerun = false,
        )
    }

    private suspend fun List<Descriptor>.filterForAutoRun() =
        filter { it.enabled }
            .map { descriptor ->
                descriptor.copy(
                    netTests = descriptor.netTests
                        .filter { netTest -> descriptor.isEnabledForAutoRun(netTest) },
                    longRunningTests = descriptor.longRunningTests
                        .filter { netTest -> descriptor.isEnabledForAutoRun(netTest) },
                )
            }
            // We only want descriptors with any test left
            .filter { it.netTests.any() || it.longRunningTests.any() }

    private suspend fun Descriptor.isEnabledForAutoRun(netTest: NetTest) =
        preferenceRepository.isNetTestEnabled(this, netTest, isAutoRun = true).first()
}

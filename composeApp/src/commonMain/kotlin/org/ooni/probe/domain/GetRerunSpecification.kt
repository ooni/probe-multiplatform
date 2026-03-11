package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.ResultItem
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunSpecification

class GetRerunSpecification(
    private val getResult: (resultId: ResultModel.Id) -> Flow<ResultItem?>,
) {
    suspend operator fun invoke(spec: RunSpecification.Rerun): RunSpecification.Full? {
        val resultItem = getResult(spec.resultId).first() ?: return null

        return RunSpecification.Full(
            tests = listOf(
                RunSpecification.Test(
                    descriptorId = resultItem.descriptor.descriptor.id,
                    netTests = listOf(
                        NetTest(
                            test = TestType.WebConnectivity,
                            inputs = resultItem.measurements.mapNotNull { it.url?.url },
                        ),
                    ),
                ),
            ),
            taskOrigin = TaskOrigin.OoniRun,
            isRerun = true,
        )
    }
}

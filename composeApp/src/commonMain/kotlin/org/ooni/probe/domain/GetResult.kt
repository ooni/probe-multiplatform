package org.ooni.probe.domain

import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.repositories.ResultRepository

class GetResult(
    private val resultRepository: ResultRepository,
) {
    operator fun invoke(resultId: ResultModel.Id) = resultRepository.getById(resultId)
}

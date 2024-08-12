package org.ooni.probe.domain

import org.ooni.probe.data.repositories.ResultRepository

class GetResults(
    private val resultRepository: ResultRepository,
) {
    operator fun invoke() = resultRepository.listWithNetwork()
}

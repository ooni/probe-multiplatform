package org.ooni.probe.data.models

data class ResultsStats(
    val total: Long,
    val networks: Long,
    val dataUsageUp: Long,
    val dataUsageDown: Long,
)

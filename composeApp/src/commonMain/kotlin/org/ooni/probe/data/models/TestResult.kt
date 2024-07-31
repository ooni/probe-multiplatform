package org.ooni.probe.data.models

data class TestResult(
    val id: Id,
) {
    data class Id(val value: String)
}

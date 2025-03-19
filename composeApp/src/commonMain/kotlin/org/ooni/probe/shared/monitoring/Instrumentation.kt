package org.ooni.probe.shared.monitoring

expect object Instrumentation {
    suspend fun <T> withTransaction(
        operation: String,
        name: String? = null,
        data: Map<String, Any> = emptyMap(),
        block: suspend () -> T,
    ): T
}

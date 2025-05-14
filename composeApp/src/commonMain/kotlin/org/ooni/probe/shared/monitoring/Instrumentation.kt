package org.ooni.probe.shared.monitoring

expect object Instrumentation {
    suspend fun <T> withTransaction(
        operation: String,
        name: String? = null,
        data: Map<String, Any> = emptyMap(),
        block: suspend () -> T,
    ): T
}

suspend fun Instrumentation.reportTransaction(
    operation: String,
    name: String? = null,
    data: Map<String, Any> = emptyMap(),
) = withTransaction(operation, name, data) {}

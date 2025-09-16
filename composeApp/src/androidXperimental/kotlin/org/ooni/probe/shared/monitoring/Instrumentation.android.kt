package org.ooni.probe.shared.monitoring

actual object Instrumentation {
    actual suspend fun <T> withTransaction(
        operation: String,
        name: String?,
        data: Map<String, Any>,
        block: suspend () -> T,
    ): T = block()
}

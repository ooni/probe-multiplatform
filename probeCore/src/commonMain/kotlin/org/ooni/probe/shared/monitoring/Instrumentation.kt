package org.ooni.probe.shared.monitoring

/**
 * Performance/telemetry instrumentation used by probeCore's engine and domain layer.
 *
 * probeCore keeps only the API and a no-op default so it stays free of any monitoring SDK. The
 * hosting app installs a real (e.g. Sentry-backed) [InstrumentationDelegate] at startup via
 * [Instrumentation.delegate]; the CLI leaves the no-op in place.
 */
object Instrumentation {
    var delegate: InstrumentationDelegate = NoOpInstrumentationDelegate

    suspend fun <T> withTransaction(
        operation: String,
        name: String? = null,
        data: Map<String, Any> = emptyMap(),
        block: suspend () -> T,
    ): T = delegate.withTransaction(operation, name, data, block)
}

interface InstrumentationDelegate {
    suspend fun <T> withTransaction(
        operation: String,
        name: String?,
        data: Map<String, Any>,
        block: suspend () -> T,
    ): T
}

object NoOpInstrumentationDelegate : InstrumentationDelegate {
    override suspend fun <T> withTransaction(
        operation: String,
        name: String?,
        data: Map<String, Any>,
        block: suspend () -> T,
    ): T = block()
}

suspend fun Instrumentation.reportTransaction(
    operation: String,
    name: String? = null,
    data: Map<String, Any> = emptyMap(),
) = withTransaction(operation, name, data) {}

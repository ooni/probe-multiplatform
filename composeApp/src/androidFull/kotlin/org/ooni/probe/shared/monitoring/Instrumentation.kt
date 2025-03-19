package org.ooni.probe.shared.monitoring

import io.sentry.Sentry
import io.sentry.TransactionOptions

actual object Instrumentation {
    actual suspend fun <T> withTransaction(
        operation: String,
        name: String?,
        data: Map<String, Any>,
        block: suspend () -> T,
    ): T {
        if (!Sentry.isEnabled()) return block()

        val span = Sentry.getSpan()
        return if (span == null) {
            val transaction = Sentry.startTransaction(
                name ?: operation,
                operation,
                TransactionOptions().also { it.isBindToScope = true },
            )
            data.forEach { (key, value) -> transaction.setData(key, value) }
            val result = block()
            transaction.finish()
            result
        } else {
            val innerSpan = span.startChild(operation)
            data.forEach { (key, value) -> innerSpan.setData(key, value) }
            val result = block()
            innerSpan.finish()
            result
        }
    }
}

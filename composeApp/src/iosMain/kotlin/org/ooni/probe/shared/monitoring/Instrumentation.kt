package org.ooni.probe.shared.monitoring

import cocoapods.Sentry.SentrySDK

actual object Instrumentation {
    actual suspend fun <T> withTransaction(
        operation: String,
        name: String?,
        data: Map<String, Any>,
        block: suspend () -> T,
    ): T {
        if (!SentrySDK.isEnabled()) return block()

        val span = SentrySDK.span
        return if (span == null) {
            val transaction = SentrySDK.startTransactionWithName(
                name = name ?: operation,
                operation = operation,
                bindToScope = true,
            )
            data.forEach { (k, v) -> transaction.setDataValue(value = v, forKey = k) }
            val result = block()
            transaction.finish()
            result
        } else {
            val innerSpan = span.startChildWithOperation(operation)
            data.forEach { (k, v) -> innerSpan.setDataValue(value = v, forKey = k) }
            val result = block()
            innerSpan.finish()
            result
        }
    }
}

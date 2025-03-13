package org.ooni.probe.background

fun interface BackgroundRunner {
    operator fun invoke(
        longRunningProcess: (() -> Unit),
        completionHandler: (() -> Unit)?,
        cancellationHandler: (() -> Unit)?,
    )
}

package org.ooni.probe.background

fun interface BackgroundRunner {
    operator fun invoke(
        background: (() -> Unit),
        cancel: (() -> Unit),
    )
}

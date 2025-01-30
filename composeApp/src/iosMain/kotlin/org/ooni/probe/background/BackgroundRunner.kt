package org.ooni.probe.background

fun interface BackgroundRunner {
    operator fun invoke(
        onDoInBackground: (() -> Unit),
        onCancel: (() -> Unit),
    )
}

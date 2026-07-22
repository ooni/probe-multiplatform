package org.ooni.passport.models

fun Throwable?.isOfflineFailure(): Boolean {
    var current = this
    var depth = 0
    while (current != null && depth < MAX_CAUSE_DEPTH) {
        if (current is PassportException.Offline) return true
        current = current.cause
        depth++
    }
    return false
}

// Guards against self-referencing cause chains, which would otherwise loop forever.
private const val MAX_CAUSE_DEPTH = 8

package org.ooni.probe.ui.shared

fun String.ellipsize(numChars: Int) =
    if (length <= numChars) {
        this
    } else {
        take(numChars) + Typography.ellipsis
    }

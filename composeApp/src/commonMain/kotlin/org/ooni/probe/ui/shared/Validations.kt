package org.ooni.probe.ui.shared

fun String.isValidUrl() =
    Regex("^https?://[\\w-.]+?\\..{2,}")
        .matches(this)

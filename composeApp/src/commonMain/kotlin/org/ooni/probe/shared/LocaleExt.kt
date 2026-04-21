package org.ooni.probe.shared

import androidx.compose.ui.text.intl.Locale

val Locale.languageRegionString
    get() = language + region.ifBlank { null }?.let { "-$it" }.orEmpty()

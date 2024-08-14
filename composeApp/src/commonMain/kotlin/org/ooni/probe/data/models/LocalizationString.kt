package org.ooni.probe.data.models

import androidx.compose.ui.text.intl.Locale

typealias LocalizationString = Map<String, String>

fun LocalizationString.getCurrent() = get(Locale.current.language)

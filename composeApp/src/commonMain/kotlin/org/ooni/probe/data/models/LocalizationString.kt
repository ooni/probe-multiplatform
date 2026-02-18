package org.ooni.probe.data.models

import androidx.compose.ui.text.intl.Locale

typealias LocalizationString = Map<String, String>

/*
 Lookup order:
 - pt_BR only matches pt_BR
 - pt only matches pt
 - pt also matches pt_BR
 */
fun LocalizationString.getCurrent() =
    get(Locale.current.language + "_" + Locale.current.region)
        ?: entries.firstOrNull { it.key == Locale.current.language }?.value
        ?: entries.firstOrNull { it.key.startsWith(Locale.current.language) }?.value

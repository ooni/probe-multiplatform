package org.ooni.probe.data.models

import androidx.compose.ui.text.intl.Locale
import org.ooni.probe.config.OrganizationConfig

typealias LocalizationString = Map<String, String>

/*
 Lookup order:
 - pt_BR only matches pt_BR
 - pt only matches pt
 - pt also matches pt_BR

 If the OS language is not in supportedLanguageCodes, falls back to English
 to avoid showing content in an unsupported language.
 */
fun LocalizationString.getCurrent(): String? {
    val osLanguage = Locale.current.language
    val language =
        if (osLanguage in OrganizationConfig.supportedLanguageCodes) osLanguage else "en"
    val region = if (language == osLanguage) Locale.current.region else ""
    return get(language + "_" + region)
        ?: entries.firstOrNull { it.key == language }?.value
        ?: entries.firstOrNull { it.key.startsWith(language) }?.value
}

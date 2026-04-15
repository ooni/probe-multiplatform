package org.ooni.probe.data.models

import androidx.compose.ui.text.intl.Locale
import org.ooni.probe.config.OrganizationConfig

typealias LocalizationString = Map<String, String>

/*
 Lookup order:
 - pt_BR matches pt_BR, fallbacks to pt
 - pt also matches pt_BR

 If the OS language is not in supportedLanguageCodes, falls back to English
 to avoid showing content in an unsupported language.
 */
fun LocalizationString.getCurrent(
    currentLanguage: String = Locale.current.language,
    currentRegion: String = Locale.current.region,
): String? {
    val language =
        if (currentLanguage in OrganizationConfig.supportedLanguageCodes) currentLanguage else "en"
    val region = if (language == currentLanguage) currentRegion else ""
    return get(language + "_" + region)
        ?: entries.firstOrNull { it.key == language }?.value
        ?: entries.firstOrNull { it.key.startsWith(language) }?.value
}

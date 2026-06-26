package org.ooni.probe.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.ooni.probe.data.models.SettingsKey
import java.util.Locale

class DesktopLocaleController(
    private val getValueByKey: (SettingsKey) -> Flow<Any?>,
    private val systemDefaultLocale: Locale,
) {
    private val initialLanguage: String? =
        runBlocking {
            (getValueByKey(SettingsKey.LANGUAGE_SETTING).first() as? String)
                ?.takeIf { it.isNotBlank() }
        }

    fun applyInitialLocale() {
        initialLanguage?.let { Locale.setDefault(Locale.forLanguageTag(it)) }
    }

    @Composable
    fun currentLocale(): Locale {
        val selectedLanguage by getValueByKey(SettingsKey.LANGUAGE_SETTING)
            .map { (it as? String)?.takeIf { code -> code.isNotBlank() } }
            .collectAsState(initial = initialLanguage)
        val targetLocale =
            selectedLanguage?.let { Locale.forLanguageTag(it) } ?: systemDefaultLocale
        // Applied inline (not in a SideEffect) so the default is set before the keyed subtree below
        // resolves its string resources; a SideEffect would run too late and leave stale strings.
        if (Locale.getDefault() != targetLocale) {
            Locale.setDefault(targetLocale)
        }
        return targetLocale
    }
}

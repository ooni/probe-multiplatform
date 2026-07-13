package org.ooni.probe.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.shared.LanguageSupport

class LocaleController(
    private val getValueByKey: (SettingsKey) -> Flow<Any?>,
    private val languageSupport: LanguageSupport,
    private val platformLocale: PlatformLocale = buildPlatformLocale(),
) {
    private val isEnabled = languageSupport == LanguageSupport.IN_APP

    private var lastAppliedTag: String? = null

    /** Call once at start-up, before the first composition or `getString` call. */
    suspend fun applyInitialLocale() {
        if (!isEnabled) return
        apply(getValueByKey(SettingsKey.LANGUAGE_SETTING).first().asLanguageTag())
    }

    @Composable
    fun currentLanguageTag(): String {
        if (!isEnabled) return platformLocale.currentTag()

        val selectedLanguage by getValueByKey(SettingsKey.LANGUAGE_SETTING)
            .map { it.asLanguageTag() }
            .collectAsState(initial = lastAppliedTag)
        return apply(selectedLanguage)
    }

    private fun apply(tag: String?): String {
        lastAppliedTag = tag
        val target = tag ?: platformLocale.systemDefaultTag
        if (platformLocale.currentTag() != target) {
            platformLocale.setDefault(target)
        }
        return target
    }

    private fun Any?.asLanguageTag() = (this as? String)?.takeIf { it.isNotBlank() }
}

package org.ooni.probe.locale

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.shared.LanguageSupport
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleControllerTest {
    private val language = MutableStateFlow<Any?>(null)
    private val platformLocale = FakePlatformLocale(systemDefaultTag = "en-GB")

    private fun buildController(languageSupport: LanguageSupport = LanguageSupport.IN_APP) =
        LocaleController(
            getValueByKey = { key ->
                check(key == SettingsKey.LANGUAGE_SETTING)
                language
            },
            languageSupport = languageSupport,
            platformLocale = platformLocale,
        )

    @Test
    fun appliesStoredLanguage() =
        runTest {
            language.value = "pt-BR"

            buildController().applyInitialLocale()

            assertEquals("pt-BR", platformLocale.currentTag())
        }

    @Test
    fun fallsBackToSystemDefaultWhenNoLanguageStored() =
        runTest {
            // LanguageViewModel writes a blank value for the "System default" option
            language.value = ""
            platformLocale.setDefault("pt-BR")

            buildController().applyInitialLocale()

            assertEquals("en-GB", platformLocale.currentTag())
        }

    @Test
    fun doesNotOverrideWhenThePlatformOwnsTheLanguage() =
        runTest {
            // The OS already handed us the language it picked for the app
            platformLocale.setDefault("pt-BR")
            language.value = "es"

            buildController(LanguageSupport.SYSTEM_SETTINGS).applyInitialLocale()

            assertEquals("pt-BR", platformLocale.currentTag())
        }

    private class FakePlatformLocale(
        override val systemDefaultTag: String,
    ) : PlatformLocale {
        private var currentTag = systemDefaultTag

        override fun currentTag() = currentTag

        override fun setDefault(tag: String) {
            currentTag = tag
        }
    }
}

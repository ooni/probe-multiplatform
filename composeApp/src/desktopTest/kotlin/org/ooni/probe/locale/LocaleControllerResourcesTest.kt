package org.ooni.probe.locale

import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Title
import org.jetbrains.compose.resources.getString
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.shared.LanguageSupport
import org.ooni.testing.createPreferenceDataStore
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The point of the whole locale controller: that a stored language actually reaches Compose
 * Resources. Only a real [PreferenceRepository] and the real platform locale can prove that.
 */
class LocaleControllerResourcesTest {
    private val systemDefaultLocale: Locale = Locale.getDefault()
    private val preferenceRepository = PreferenceRepository(createPreferenceDataStore())

    @AfterTest
    fun tearDown() {
        Locale.setDefault(systemDefaultLocale)
    }

    @Test
    fun storedLanguageIsUsedToResolveStringResources() =
        runTest {
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, "es")

            buildController().applyInitialLocale()

            assertEquals("Configuración", getString(Res.string.Settings_Title))
        }

    @Test
    fun platformsWhereTheOsOwnsTheLanguageKeepResolvingWithTheSystemOne() =
        runTest {
            Locale.setDefault(Locale.forLanguageTag("en"))
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, "es")

            buildController(LanguageSupport.SYSTEM_SETTINGS).applyInitialLocale()

            assertEquals("Settings", getString(Res.string.Settings_Title))
        }

    private fun buildController(languageSupport: LanguageSupport = LanguageSupport.IN_APP) =
        LocaleController(
            getValueByKey = preferenceRepository::getValueByKey,
            languageSupport = languageSupport,
        )
}

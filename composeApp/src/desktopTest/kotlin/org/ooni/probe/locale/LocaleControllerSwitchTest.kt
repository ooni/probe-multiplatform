package org.ooni.probe.locale

import androidx.compose.material3.Text
import androidx.compose.runtime.key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlinx.coroutines.runBlocking
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Title
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.shared.LanguageSupport
import org.ooni.testing.createPreferenceDataStore
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Picking a language in Settings has to relocalize the running UI, without a restart. This drives
 * the same shape the platform entry points use: key(currentLanguageTag()) around the content.
 */
class LocaleControllerSwitchTest {
    private val systemDefaultLocale: Locale = Locale.getDefault()
    private val preferenceRepository = PreferenceRepository(createPreferenceDataStore())

    @AfterTest
    fun tearDown() {
        Locale.setDefault(systemDefaultLocale)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun changingTheLanguagePreferenceRelocalizesTheRunningUi() {
        Locale.setDefault(Locale.forLanguageTag("en"))
        val controller = LocaleController(
            getValueByKey = preferenceRepository::getValueByKey,
            languageSupport = LanguageSupport.IN_APP,
        )

        runDesktopComposeUiTest {
            setContent {
                key(controller.currentLanguageTag()) {
                    Text(stringResource(Res.string.Settings_Title))
                }
            }

            onNodeWithText("Settings").assertIsDisplayed()

            runBlocking {
                preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, "es")
            }
            waitForIdle()

            onNodeWithText("Configuración").assertIsDisplayed()
        }
    }
}

package org.ooni.probe.ui.settings.language

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LanguageViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())

    // Region-qualified tags, intentionally unsorted, with display names that don't match code order.
    private val supportedLanguages = listOf("pt-BR", "en", "pt-PT")
    private val languageNames = mapOf(
        "pt-BR" to "Português (Brasil)",
        "en" to "English",
        "pt-PT" to "Português (Portugal)",
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun optionsBuiltFromRegionTagsSortedByName() =
        runTest(dispatcher) {
            val viewModel = buildViewModel()

            val options = viewModel.state.first().options
            assertEquals(
                listOf("en", "pt-BR", "pt-PT"),
                options.map { it.code },
            )
            assertEquals(
                listOf("English", "Português (Brasil)", "Português (Portugal)"),
                options.map { it.name },
            )
        }

    @Test
    fun selectedLanguageReflectsStoredPreference() =
        runTest(dispatcher) {
            val viewModel = buildViewModel(initialLanguage = "pt-BR")

            assertEquals("pt-BR", viewModel.state.first().selectedLanguage)
        }

    @Test
    fun blankPreferenceMeansSystemDefault() =
        runTest(dispatcher) {
            val viewModel = buildViewModel(initialLanguage = "")

            assertNull(viewModel.state.first().selectedLanguage)
        }

    @Test
    fun optionSelectedStoresCodeAndUpdatesState() =
        runTest(dispatcher) {
            val viewModel = buildViewModel()

            viewModel.onEvent(LanguageViewModel.Event.OptionSelected("pt-PT"))

            assertEquals("pt-PT", viewModel.state.first().selectedLanguage)
        }

    @Test
    fun systemDefaultSelectionStoresEmptyAndClearsState() =
        runTest(dispatcher) {
            val viewModel = buildViewModel(initialLanguage = "pt-BR")

            viewModel.onEvent(LanguageViewModel.Event.OptionSelected(null))

            assertNull(viewModel.state.first().selectedLanguage)
        }

    @Test
    fun backClicked() =
        runTest(dispatcher) {
            var backPressed = false
            val viewModel = buildViewModel(onBack = { backPressed = true })

            viewModel.onEvent(LanguageViewModel.Event.BackClicked)
            assertTrue(backPressed)
        }

    private fun buildViewModel(
        onBack: () -> Unit = {},
        initialLanguage: String? = null,
    ): LanguageViewModel {
        val preference = MutableStateFlow<Any?>(initialLanguage)
        return LanguageViewModel(
            onBack = onBack,
            supportedLanguages = supportedLanguages,
            getLanguageName = { code -> languageNames[code] ?: code },
            getPreference = { preference },
            setPreference = { _, value -> preference.value = value },
        )
    }
}

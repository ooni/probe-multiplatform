package org.ooni.probe.screenshots

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ooniprobe.composeapp.generated.resources.Common_Back
import ooniprobe.composeapp.generated.resources.Dashboard_Overview_ChooseWebsites
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_Description
import ooniprobe.composeapp.generated.resources.OONIRun_Run
import ooniprobe.composeapp.generated.resources.Onboarding_AutomatedTesting_Title
import ooniprobe.composeapp.generated.resources.Onboarding_Crash_Title
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Title
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_True
import ooniprobe.composeapp.generated.resources.Onboarding_ThingsToKnow_Button
import ooniprobe.composeapp.generated.resources.Onboarding_ThingsToKnow_Title
import ooniprobe.composeapp.generated.resources.Onboarding_WhatIsOONIProbe_GotIt
import ooniprobe.composeapp.generated.resources.Onboarding_WhatIsOONIProbe_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_About_Label
import ooniprobe.composeapp.generated.resources.Settings_Advanced_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_Label
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Label
import ooniprobe.composeapp.generated.resources.Settings_Sharing_UploadResults_Description
import ooniprobe.composeapp.generated.resources.Settings_TestOptions_Label
import ooniprobe.composeapp.generated.resources.Settings_Title
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Label
import ooniprobe.composeapp.generated.resources.Settings_Websites_CustomURL_Title
import ooniprobe.composeapp.generated.resources.TestResults
import ooniprobe.composeapp.generated.resources.Tests_Title
import ooniprobe.composeapp.generated.resources.app_name
import org.ooni.probe.App
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.OoniTest
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.toDescriptorItem
import org.ooni.probe.di.Dependencies
import org.ooni.testing.TestLifecycleOwner
import org.ooni.testing.defaultSettings
import org.ooni.testing.disableRefreshArticles
import org.ooni.testing.factories.DatabaseHelper
import org.ooni.testing.skipOnboarding
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Captures Compose Desktop screenshots driven by the same `DatabaseHelper.setup()`
 * fixture that fastlane android capture_screens uses.
 *
 * Locales come from the `ooni.screenshots.locales` system property (comma-separated, default
 * `en-US`); output goes to `ooni.screenshots.outputDir/<locale>/<name>.png`.
 */
@OptIn(ExperimentalTestApi::class)
class DesktopScreenshotsTest {
    @BeforeTest
    fun setUp() {
        runBlocking {
            DatabaseHelper.setup()
            // The onboarding() test flips FIRST_RUN=true; re-skip onboarding so other
            // tests land on the dashboard.
            skipOnboarding(dependencies.preferenceRepository)
        }
    }

    @Test
    fun onboarding() =
        perLocale { locale ->
            runBlocking {
                dependencies.preferenceRepository.setValueByKey(SettingsKey.FIRST_RUN, true)
            }

            renderApp()

            waitForText(string(Res.string.Onboarding_WhatIsOONIProbe_Title))
            capture(locale, "00-what-is-ooni-probe")

            clickText(string(Res.string.Onboarding_WhatIsOONIProbe_GotIt))
            waitForText(string(Res.string.Onboarding_ThingsToKnow_Title))
            capture(locale, "01-things-to-know")

            clickText(string(Res.string.Onboarding_ThingsToKnow_Button))
            clickText(string(Res.string.Onboarding_PopQuiz_True))
            clickText(string(Res.string.Onboarding_PopQuiz_True))

            waitForText(string(Res.string.Onboarding_AutomatedTesting_Title))
            capture(locale, "02-automated-testing")

            clickTag("No-AutoTest")
            waitForText(string(Res.string.Onboarding_Crash_Title))
            capture(locale, "03-crash-reporting")

            clickTag("Yes-CrashReporting")
            // 04-enable-notifications is skipped on desktop: PlatformInfo.requestNotificationsPermission
            // is false, so the notifications onboarding step never renders.
            waitForText(string(Res.string.Onboarding_DefaultSettings_Title))
            capture(locale, "05-default-settings")
        }

    // 07-running-running is intentionally omitted on desktop. ScreenshotDependencies wires a
    // stub OonimkallBridge and a no-op startSingleRunInner, so clicking Run-Button never
    // transitions the UI into the Dashboard_Running_Running state. Android remains the sole
    // producer of that asset.

    @Test
    fun dashboard() =
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            capture(locale, "06-dashboard")
        }

    @Test
    fun tests() {
        if (!isOoni) return
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.Tests_Title))
            waitForText(websitesTitle())
            capture(locale, "22-tests")
        }
    }

    @Test
    fun runTests() =
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.OONIRun_Run))
            waitForText(string(Res.string.Dashboard_RunTests_Description))
            capture(locale, "06-run-tests")
        }

    @Test
    fun results() {
        if (!isOoni) return
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.TestResults))
            waitForText(websitesTitle(), timeout = LONG_WAIT)
            capture(locale, "17-results")
        }
    }

    @Test
    fun websitesResults() {
        if (!isOoni) return
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.TestResults))
            val websites = websitesTitle()
            clickText(websites, timeout = LONG_WAIT)
            // Narrow window may ellipsize URLs; back button is a stable signal the sub-screen mounted.
            waitForContentDescription(string(Res.string.Common_Back), timeout = LONG_WAIT)
            capture(locale, "18-websites-results")
        }
    }

    @Test
    fun chooseWebsites() {
        if (!isOoni) return
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.Tests_Title))
            clickText(websitesTitle())
            clickText(string(Res.string.Dashboard_Overview_ChooseWebsites))
            waitForText(string(Res.string.Settings_Websites_CustomURL_Title))
            capture(locale, "21-choose-websites")
        }
    }

    @Test
    fun settings() =
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.Settings_Title))
            waitForText(string(Res.string.Settings_About_Label))
            capture(locale, "09-settings")
        }

    @Test
    fun settingsTestOptions() =
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.Settings_Title))
            clickText(string(Res.string.Settings_TestOptions_Label))
            waitForText(string(Res.string.Settings_Sharing_UploadResults_Description))
            capture(locale, "11-test-options")
        }

    @Test
    fun settingsWebsiteCategories() {
        if (!isOoni) return
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.Settings_Title))
            clickText(string(Res.string.Settings_TestOptions_Label))
            clickText(string(Res.string.Settings_Websites_Categories_Label))
            waitForText(string(Res.string.Settings_Websites_Categories_Label))
            capture(locale, "12-websites-categories")
        }
    }

    @Test
    fun settingsPrivacy() =
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.Settings_Title))
            clickText(string(Res.string.Settings_Privacy_Label))
            waitForText(string(Res.string.Settings_Privacy_Label))
            capture(locale, "13-privacy")
        }

    @Test
    fun settingsProxy() =
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.Settings_Title))
            clickText(string(Res.string.Settings_Proxy_Label))
            // Psiphon is gated on isPsiphonSupported (false on desktop); wait for the
            // back button instead — that's a stable signal that the sub-screen mounted.
            waitForContentDescription(string(Res.string.Common_Back))
            capture(locale, "14-proxy")
        }

    @Test
    fun settingsAdvanced() =
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.Settings_Title))
            clickText(string(Res.string.Settings_Advanced_Label))
            waitForText(string(Res.string.Settings_Advanced_Label))
            capture(locale, "15-advanced")
        }

    @Test
    fun settingsAbout() =
        perLocale { locale ->
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.Settings_Title))
            clickText(string(Res.string.Settings_About_Label))
            waitForTag("AboutScreen")
            capture(locale, "16-about")
        }

    private fun perLocale(block: ComposeUiTest.(locale: String) -> Unit) {
        val previousLocale = Locale.getDefault()
        try {
            for (tag in screenshotLocales()) {
                Locale.setDefault(Locale.forLanguageTag(tag))
                // Match Main.kt's minimum window size (Dimension(320, 560)) so screenshots
                // reflect the smallest supported app layout rather than the runComposeUiTest
                // default (1024x768).
                runDesktopComposeUiTest(width = WINDOW_WIDTH, height = WINDOW_HEIGHT) {
                    block(tag)
                }
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    private fun ComposeUiTest.renderApp() {
        setContent {
            CompositionLocalProvider(
                LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED),
            ) {
                App(dependencies = dependencies, deepLink = null)
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.capture(
        locale: String,
        name: String,
    ) {
        waitForIdle()
        val image = onRoot().captureToImage().toAwtImage()
        val outDir = File(outputRoot(), locale).apply { mkdirs() }
        ImageIO.write(image, "png", File(outDir, "$name.png"))
    }

    private val isOoni: Boolean
        get() = OrganizationConfig.baseSoftwareName.contains("ooni")

    private fun websitesTitle(): String =
        runBlocking {
            dependencies.testDescriptorRepository
                .listLatestByIds(listOf(Descriptor.Id(OoniTest.Websites.id)))
                .first()
                .firstOrNull()
                ?.toDescriptorItem()
                ?.title
                ?.invoke()
                ?: "Websites"
        }

    private fun outputRoot(): File {
        val configured = System.getProperty(OUTPUT_DIR_PROPERTY)
        val target = if (configured.isNullOrBlank()) {
            File("build/screenshots")
        } else {
            File(configured)
        }
        target.mkdirs()
        return target
    }

    private fun screenshotLocales(): List<String> {
        val raw = System.getProperty(LOCALES_PROPERTY)
        if (raw.isNullOrBlank()) return listOf(DEFAULT_LOCALE)
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }

    companion object {
        private const val OUTPUT_DIR_PROPERTY = "ooni.screenshots.outputDir"
        private const val LOCALES_PROPERTY = "ooni.screenshots.locales"
        private const val DEFAULT_LOCALE = "en-US"

        // Mirror the minimum window size enforced by Main.kt (Dimension(320, 560)).
        private const val WINDOW_WIDTH = 480
        private const val WINDOW_HEIGHT = 800

        // Shared per JVM: a single tempDir + Dependencies survives every @Test method.
        // DatabaseHelper.initialize() ignores subsequent calls (singleton), so we only
        // get one chance to wire it; per-test re-seeding goes through DatabaseHelper.setup().
        private val workingDir: Path =
            Files.createTempDirectory("ooni-screenshots-").also { dir ->
                Runtime.getRuntime().addShutdownHook(
                    Thread { dir.toFile().deleteRecursively() },
                )
            }

        private val dependencies: Dependencies =
            buildScreenshotDependencies(workingDir).also { deps ->
                DatabaseHelper.initialize(deps)
                runBlocking {
                    deps.bootstrapTestDescriptors()
                    deps.bootstrapPreferences()
                    deps.preferenceRepository.setValueByKey(SettingsKey.SEND_CRASH, false)
                    disableRefreshArticles(deps.preferenceRepository)
                    skipOnboarding(deps.preferenceRepository)
                    defaultSettings(deps.preferenceRepository)
                }
            }
    }
}

package org.ooni.probe.screenshots

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
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
import ooniprobe.composeapp.generated.resources.Test_Dash_Fullname
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
    fun websiteMeasurementAnomaly() {
        if (!isOoni) return
        perLocale { locale ->
            preloadMeasurementSnapshot(YOUTUBE_MEASUREMENT_UID, YOUTUBE_URL)
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.TestResults))
            clickText(websitesTitle(), timeout = LONG_WAIT)
            clickText(YOUTUBE_URL, timeout = LONG_WAIT)
            waitForTag(EXPLORER_SNAPSHOT_TAG, timeout = LONG_WAIT)
            capture(locale, "19-website-measurement-anomaly")
        }
    }

    @Test
    fun dashMeasurement() {
        if (!isOoni) return
        perLocale { locale ->
            preloadMeasurementSnapshot(DASH_MEASUREMENT_UID, DASH_MARKER)
            renderApp()
            waitForContentDescription(string(Res.string.app_name))
            clickText(string(Res.string.TestResults))
            clickText(performanceTitle(), timeout = LONG_WAIT)
            clickText(string(Res.string.Test_Dash_Fullname), timeout = LONG_WAIT)
            waitForTag(EXPLORER_SNAPSHOT_TAG, timeout = LONG_WAIT)
            capture(locale, "20-dash-measurement")
        }
    }

    private fun preloadMeasurementSnapshot(
        uid: String,
        expectedDomText: String,
    ) {
        val (_, _, density) = screenshotViewport()
        // Snapshot at the production app's actual window dp (Main.kt → 480×800), scaled by the
        // active density. This matches what real users see — explorer.ooni.org adapts to that
        // viewport — and stays crisp when the Mac App Store retina pipeline downscales it to fit
        // the framed window inside `MacScreenshotFrame`.
        val widthPx = (APP_WINDOW_DP_WIDTH * density).toInt()
        val heightPx = ((APP_WINDOW_DP_HEIGHT - MEASUREMENT_TOP_BAR_DP) * density).toInt()
        val url = explorerMeasurementUrl(uid)
        runBlocking {
            preloadExplorerSnapshot(
                url = url,
                widthPx = widthPx,
                heightPx = heightPx,
                expectedDomText = expectedDomText,
            )
        }
    }

    private fun explorerMeasurementUrl(uid: String): String {
        val locale = Locale.getDefault()
        val languageRegion = buildString {
            append(locale.language.ifEmpty { "en" })
            if (locale.country.isNotEmpty()) append('-').append(locale.country)
        }
        return "${OrganizationConfig.explorerUrl}/m/$uid?webview=true&language=$languageRegion"
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
        val (width, height, _) = screenshotViewport()
        try {
            for (tag in screenshotLocales()) {
                Locale.setDefault(Locale.forLanguageTag(tag))
                // Viewport sized via ooni.screenshots.{width,height,density} (defaults
                // 480x800@1x to mirror Main.kt's minimum window). desktopCaptureScreensMacAppStore
                // overrides to 2560x1600@2x for retina Mac App Store assets.
                runDesktopComposeUiTest(width = width, height = height) {
                    block(tag)
                }
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    private fun ComposeUiTest.renderApp() {
        val (_, _, density) = screenshotViewport()
        val chrome = System.getProperty(CHROME_PROPERTY).orEmpty()
        setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = density, fontScale = 1f),
                LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED),
            ) {
                if (chrome == CHROME_MAC) {
                    MacScreenshotFrame {
                        App(dependencies = dependencies, deepLink = null)
                    }
                } else {
                    App(dependencies = dependencies, deepLink = null)
                }
            }
        }
        waitForIdle()
    }

    private fun screenshotViewport(): Triple<Int, Int, Float> {
        val width = System.getProperty(WIDTH_PROPERTY)?.toIntOrNull() ?: DEFAULT_WIDTH
        val height = System.getProperty(HEIGHT_PROPERTY)?.toIntOrNull() ?: DEFAULT_HEIGHT
        val density = System.getProperty(DENSITY_PROPERTY)?.toFloatOrNull() ?: DEFAULT_DENSITY
        return Triple(width, height, density)
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

    private fun websitesTitle(): String = descriptorTitle(OoniTest.Websites.id, fallback = "Websites")

    private fun performanceTitle(): String = descriptorTitle(OoniTest.Performance.id, fallback = "Performance")

    private fun descriptorTitle(
        id: String,
        fallback: String,
    ): String =
        runBlocking {
            dependencies.testDescriptorRepository
                .listLatestByIds(listOf(Descriptor.Id(id)))
                .first()
                .firstOrNull()
                ?.toDescriptorItem()
                ?.title
                ?.invoke()
                ?: fallback
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
        private const val YOUTUBE_URL = "https://www.youtube.com/"
        private const val DASH_MARKER = "2160p"
        private const val EXPLORER_SNAPSHOT_TAG = TAG

        // Must stay in sync with `DatabaseHelper.setupOoniResults` seeded measurement UIDs.
        private const val YOUTUBE_MEASUREMENT_UID =
            "20260421120343.029379_PT_webconnectivity_ef4879ff6cfb93bc"
        private const val DASH_MEASUREMENT_UID =
            "20260421121842.831998_PT_dash_1669230bece3f8bd"

        // Material3 TopBar default height (dp); used to compute the WebView snapshot region.
        private const val MEASUREMENT_TOP_BAR_DP = 64f

        // Production desktop window size (Main.kt's DpSize default). The screenshot WebView is
        // rendered at this logical size — scaled by density — so explorer.ooni.org sees the same
        // viewport real users see.
        private const val APP_WINDOW_DP_WIDTH = 480f
        private const val APP_WINDOW_DP_HEIGHT = 800f

        private const val OUTPUT_DIR_PROPERTY = "ooni.screenshots.outputDir"
        private const val LOCALES_PROPERTY = "ooni.screenshots.locales"
        private const val WIDTH_PROPERTY = "ooni.screenshots.width"
        private const val HEIGHT_PROPERTY = "ooni.screenshots.height"
        private const val DENSITY_PROPERTY = "ooni.screenshots.density"
        private const val CHROME_PROPERTY = "ooni.screenshots.chrome"
        private const val CHROME_MAC = "mac"
        private const val DEFAULT_LOCALE = "en-US"

        // Default mirrors Main.kt's minimum window (Dimension(320, 560)).
        private const val DEFAULT_WIDTH = 480
        private const val DEFAULT_HEIGHT = 800
        private const val DEFAULT_DENSITY = 1f

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
                // Compose-native facsimile replaces JavaFX WebView; see ScreenshotOoniWebView.kt.
                installScreenshotWebViewOverride()
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

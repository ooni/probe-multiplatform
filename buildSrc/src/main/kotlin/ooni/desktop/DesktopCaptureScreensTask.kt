package ooni.desktop

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * Mac App Store Connect accepts macOS screenshots at exactly these pixel sizes.
 * The Retina pair renders the same logical layout as its non-Retina counterpart
 * at 2x density (dp viewport: 1280×800 and 1440×900 respectively).
 */
private data class MacAppStoreSize(
    val width: Int,
    val height: Int,
    val density: Float,
) {
    val key: String get() = "${width}x$height"
}

private val MAC_APP_STORE_SIZES = listOf(
    MacAppStoreSize(1280, 800, 1.0f),
    MacAppStoreSize(1440, 900, 1.0f),
    MacAppStoreSize(2560, 1600, 2.0f),
    MacAppStoreSize(2880, 1800, 2.0f),
)

private val DEFAULT_MAC_APP_STORE_SIZE = MAC_APP_STORE_SIZES.first { it.key == "2560x1600" }

private fun resolveMacAppStoreSize(raw: String?): MacAppStoreSize {
    if (raw.isNullOrBlank()) return DEFAULT_MAC_APP_STORE_SIZE
    return MAC_APP_STORE_SIZES.firstOrNull { it.key.equals(raw.trim(), ignoreCase = true) }
        ?: throw GradleException(
            "Invalid macAppStoreSize='$raw'. Allowed: " +
                MAC_APP_STORE_SIZES.joinToString { it.key },
        )
}

/**
 * Registers the `desktopCaptureScreens` task that drives `DesktopScreenshotsTest`
 * and writes screenshots to `fastlane/metadata/<organization>/desktop/<locale>/`.
 *
 * Reuses the `desktopTest` test classes directories, classpath, and Java launcher
 * so it inherits the Kotlin Multiplatform test fixture (`DatabaseHelper`-seeded
 * data — the same dataset fastlane Android `capture_screens` consumes).
 */
fun Project.registerDesktopCaptureScreensTask() {
    val organization = project.findProperty("organization") as? String
    val locales = project.findProperty("locales") as? String

    tasks.register<Test>("desktopCaptureScreens") {
        group = "verification"
        description = "Capture Compose Desktop screenshots seeded with the fastlane Android dataset."

        wireDesktopScreenshotTest()

        val outputDir = layout.projectDirectory
            .dir("../fastlane/metadata/${organization ?: "ooni"}/desktop")
            .asFile
        systemProperty("ooni.screenshots.outputDir", outputDir.absolutePath)
        locales?.let { systemProperty("ooni.screenshots.locales", it) }

        // websiteMeasurementAnomaly / dashMeasurement run a real JavaFX WebView and cache an
        // ImageBitmap per locale; 30 ooni locales × 2 tests is heavy for the default heap and
        // the worker JVM has been observed to crash mid-suite. Match the MAS task's headroom.
        maxHeapSize = "4g"

        outputs.upToDateWhen { false }
    }
}

/**
 * Registers the `desktopCaptureScreensMacAppStore` task that renders
 * `DesktopScreenshotsTest` at one of the four pixel sizes Mac App Store Connect accepts:
 * 1280×800, 1440×900, 2560×1600, or 2880×1800. Pick a size with `-PmacAppStoreSize=WxH`
 * (default: 2560×1600 retina). Density is set automatically (1× for the smaller pair,
 * 2× for the retina pair).
 *
 * Output: `fastlane/metadata/<organization>/mac-app-store/<locale>/`.
 */
fun Project.registerDesktopCaptureMacAppStoreTask() {
    val organization = project.findProperty("organization") as? String
    val locales = project.findProperty("locales") as? String
    val size = resolveMacAppStoreSize(project.findProperty("macAppStoreSize") as? String)

    tasks.register<Test>("desktopCaptureScreensMacAppStore") {
        group = "verification"
        description = "Capture Mac App Store-ready Compose Desktop screenshots (${size.key})."

        wireDesktopScreenshotTest()

        val outputDir = layout.projectDirectory
            .dir("../fastlane/metadata/${organization ?: "ooni"}/mac-app-store")
            .asFile
        systemProperty("ooni.screenshots.outputDir", outputDir.absolutePath)
        systemProperty("ooni.screenshots.width", size.width.toString())
        systemProperty("ooni.screenshots.height", size.height.toString())
        systemProperty("ooni.screenshots.density", size.density.toString())
        systemProperty("ooni.screenshots.chrome", "mac")
        locales?.let { systemProperty("ooni.screenshots.locales", it) }

        // Retina (2560x1600 / 2880x1800) captures across many locales accumulate large
        // ImageBitmaps in the test JVM; the default 512m heap OOMs partway through the suite.
        maxHeapSize = "4g"

        outputs.upToDateWhen { false }
    }
}

/**
 * Microsoft Store (Partner Center) accepts desktop screenshots between 1366×768 and
 * 3840×2160. The retina entry renders the same logical layout as its 1× counterpart at
 * 2× density (logical 1920×1080).
 */
private data class MicrosoftStoreSize(
    val width: Int,
    val height: Int,
    val density: Float,
) {
    val key: String get() = "${width}x$height"
}

private val MICROSOFT_STORE_SIZES = listOf(
    MicrosoftStoreSize(1366, 768, 1.0f),
    MicrosoftStoreSize(1920, 1080, 1.0f),
    MicrosoftStoreSize(3840, 2160, 2.0f),
)

private val DEFAULT_MICROSOFT_STORE_SIZE = MICROSOFT_STORE_SIZES.first { it.key == "1920x1080" }

private fun resolveMicrosoftStoreSize(raw: String?): MicrosoftStoreSize {
    if (raw.isNullOrBlank()) return DEFAULT_MICROSOFT_STORE_SIZE
    return MICROSOFT_STORE_SIZES.firstOrNull { it.key.equals(raw.trim(), ignoreCase = true) }
        ?: throw GradleException(
            "Invalid microsoftStoreSize='$raw'. Allowed: " +
                MICROSOFT_STORE_SIZES.joinToString { it.key },
        )
}

/**
 * Registers the `desktopCaptureScreensMicrosoftStore` task that renders
 * `DesktopScreenshotsTest` at one of the pixel sizes the Microsoft Store accepts:
 * 1366×768, 1920×1080, or 3840×2160. Pick a size with `-PmicrosoftStoreSize=WxH`
 * (default: 1920×1080). Density is set automatically (1× for the smaller pair,
 * 2× for the 3840×2160 retina size). The captured window wears a Windows 11 title bar.
 *
 * Output: `fastlane/metadata/<organization>/microsoft-store/<locale>/`.
 */
fun Project.registerDesktopCaptureMicrosoftStoreTask() {
    val organization = project.findProperty("organization") as? String
    val locales = project.findProperty("locales") as? String
    val size = resolveMicrosoftStoreSize(project.findProperty("microsoftStoreSize") as? String)

    tasks.register<Test>("desktopCaptureScreensMicrosoftStore") {
        group = "verification"
        description = "Capture Microsoft Store-ready Compose Desktop screenshots (${size.key})."

        wireDesktopScreenshotTest()

        val outputDir = layout.projectDirectory
            .dir("../fastlane/metadata/${organization ?: "ooni"}/microsoft-store")
            .asFile
        systemProperty("ooni.screenshots.outputDir", outputDir.absolutePath)
        systemProperty("ooni.screenshots.width", size.width.toString())
        systemProperty("ooni.screenshots.height", size.height.toString())
        systemProperty("ooni.screenshots.density", size.density.toString())
        systemProperty("ooni.screenshots.chrome", "windows")
        locales?.let { systemProperty("ooni.screenshots.locales", it) }

        // 3840×2160 @2× captures across many locales accumulate large ImageBitmaps in the
        // test JVM; the default 512m heap OOMs partway through the suite. Match the MAS task.
        maxHeapSize = "4g"

        outputs.upToDateWhen { false }
    }
}

/**
 * Keeps the heavy, fixture-seeded screenshot tests out of a normal `desktopTest` run. They are
 * produced on demand by the dedicated `desktopCaptureScreens*` tasks (which include them via
 * [wireDesktopScreenshotTest]); here we exclude the same package from `desktopTest` so
 * `./gradlew desktopTest` (CI unit tests) skips them.
 *
 * Lazily applied via `configureEach` (the KMP `jvm("desktop")` target creates `desktopTest`), and
 * guarded by task name so the `desktopCaptureScreens*` Test tasks keep their include-only filter.
 */
fun Project.excludeScreenshotTestsFromDesktopTest() {
    tasks.withType<Test>().configureEach {
        if (name == "desktopTest") {
            filter { excludeTestsMatching("org.ooni.probe.screenshots.*") }
        }
    }
}

private fun Test.wireDesktopScreenshotTest() {
    val project = project
    dependsOn(project.tasks.named("desktopTestClasses"))

    val desktopTest = project.tasks.named<Test>("desktopTest").get()
    testClassesDirs = desktopTest.testClassesDirs
    classpath = desktopTest.classpath
    javaLauncher.set(desktopTest.javaLauncher)

    // Optionally force the app's theme with `-Ptheme=light|dark`; unset follows the system.
    resolveScreenshotTheme(project.findProperty("theme") as? String)?.let {
        systemProperty("ooni.screenshots.theme", it)
    }

    useJUnit()
    filter {
        includeTestsMatching("org.ooni.probe.screenshots.DesktopScreenshotsTest")
    }
}

private fun resolveScreenshotTheme(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return when (raw.trim().lowercase()) {
        "light" -> "light"
        "dark" -> "dark"
        else -> throw GradleException("Invalid theme='$raw'. Allowed: light, dark")
    }
}

package ooni.desktop

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

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

private fun Test.wireDesktopScreenshotTest() {
    val project = project
    dependsOn(project.tasks.named("desktopTestClasses"))

    val desktopTest = project.tasks.named<Test>("desktopTest").get()
    testClassesDirs = desktopTest.testClassesDirs
    classpath = desktopTest.classpath
    javaLauncher.set(desktopTest.javaLauncher)

    useJUnit()
    filter {
        includeTestsMatching("org.ooni.probe.screenshots.DesktopScreenshotsTest")
    }
}

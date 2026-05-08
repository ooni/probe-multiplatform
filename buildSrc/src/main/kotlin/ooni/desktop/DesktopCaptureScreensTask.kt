package ooni.desktop

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

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

        dependsOn(tasks.named("desktopTestClasses"))

        val desktopTest = tasks.named<Test>("desktopTest").get()
        testClassesDirs = desktopTest.testClassesDirs
        classpath = desktopTest.classpath
        javaLauncher.set(desktopTest.javaLauncher)

        useJUnit()
        filter {
            includeTestsMatching("org.ooni.probe.screenshots.DesktopScreenshotsTest")
        }

        val outputDir = layout.projectDirectory
            .dir("../fastlane/metadata/${organization ?: "ooni"}/desktop")
            .asFile
        systemProperty("ooni.screenshots.outputDir", outputDir.absolutePath)
        locales?.let { systemProperty("ooni.screenshots.locales", it) }

        outputs.upToDateWhen { false }
    }
}

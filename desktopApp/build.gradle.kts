import org.gradle.api.tasks.Sync
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.time.LocalDate

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.jetbrainsComposeCompiler)

    id("ooni.common")
}
val appVersionName = libs.versions.app.versionName.get()
val appVersionCode = libs.versions.app.versionCode
    .get()
    .toInt()

// Active desktop distribution channel (resolved from -PdesktopDistribution).
val dist = distribution()

dependencies {
    implementation(project(":composeApp"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.components)
    // Main.kt hoists rememberNavController() above the locale `key()` block so navigation
    // state survives an in-app language switch; navigation-compose reaches this module only
    // transitively (implementation) via :composeApp, so depend on it directly.
    implementation(libs.navigation)
    implementation(libs.kermit)
    implementation(libs.dark.mode.detector)
    implementation(libs.bundles.desktop)
    implementation(libs.okio)
    implementation(libs.androidx.datastore.preferences.core)

    // Desktop unit tests migrated from composeApp's desktopTest.
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.coroutines.test)
}

version = appVersionName

kotlin {
    // DesktopBuildConfig is generated into :composeApp's commonMain and reaches this module
    // through `implementation(project(":composeApp"))`, so it is no longer wired in here.
    compilerOptions {
        optIn.addAll(
            "kotlin.ExperimentalStdlibApi",
            "kotlin.io.encoding.ExperimentalEncodingApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlinx.coroutines.DelicateCoroutinesApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
            "org.jetbrains.compose.resources.ExperimentalResourceApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi",
            "androidx.compose.ui.test.ExperimentalTestApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
        )
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.withType<Test>().configureEach {
    filter {
        // Mirror composeApp: only *Test classes are tests; skip helpers/fixtures
        // (screenshot frames, *Kt facades) the JUnit scanner would otherwise flag.
        includeTestsMatching("*Test")
        isFailOnNoMatchingTests = false
    }
}

// The native libraries and self-updater resources are staged by this module's own
// `prepareDesktopResources` Sync (it reads desktopApp/src/main/resources). Consume
// its output as this app's bundled resources.
val prepareDesktopResources = tasks.named<Sync>("prepareDesktopResources")

compose.desktop {
    application {
        mainClass = "org.ooni.probe.MainKt"

        // Compose Desktop runs ProGuard on the release distribution by default.
        // It mangles okio (`Okio__OkioKt.buffer$…` VerifyError), strips reflectively
        // referenced classes such as `org.sqlite.Function`, and renames JNI native
        // method names so the bundled `libsqlitejdbc.dylib` symbol `_open_utf8` no
        // longer matches — all observed at first launch of the Mac App Store build.
        // We have no desktop keep rules, and the binary-size win is marginal next to
        // the ~250 MB bundled JRE, so disable ProGuard for desktop until proper keep
        // rules exist for okio, sqlite-jdbc, JNA, and gojni.
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            if (dist.isAppStore) {
                // Pkg ships the macOS App Store build. Exe is the Windows
                // appstore-channel installer. Compose Desktop picks the format
                // that matches the current OS, so both can coexist here.
                targetFormats(TargetFormat.Pkg, TargetFormat.Exe)
            } else {
                targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
            }
            packageName = "OONI Probe"
            packageVersion = appVersionName
            description =
                "OONI Probe is a free and open source software designed to measure internet censorship and other forms of network interference."
            copyright = "© ${LocalDate.now().year} OONI. All rights reserved."
            vendor = "Open Observatory of Network Interference (OONI)"
            licenseFile = rootProject.file("LICENSE")

            modules("java.sql", "jdk.unsupported")

            // Include native libraries
            includeAllModules = true

            appResourcesRootDir.fileProvider(prepareDesktopResources.map { it.destinationDir })
            val appId = "org.ooni.probe-desktop"

            macOS {
                minimumSystemVersion = "14.8.3"
                bundleID = appId
                val macDir = if (dist.requiresSandbox) "macos/appstore" else "macos/direct"
                entitlementsFile.set(project.file("$macDir/entitlements.plist"))
                runtimeEntitlementsFile.set(project.file("$macDir/runtime-entitlements.plist"))
                infoPlist {
                    extraKeysRawXml = project
                        .file("$macDir/Info.plist")
                        .readText()
                        .replace("APP_ID", appId)
                }
                packageBuildVersion = appVersionCode.toString()
                jvmArgs("-Dapple.awt.enableTemplateImages=true") // tray template icon
                jvmArgs("-Dapple.awt.application.appearance=system") // adaptive title bar

                iconFile.set(rootProject.file("icons/app.icns"))
                appStore = dist.isAppStore
                if (dist.isAppStore) {
                    signing {
                        sign.set(true)
                        dist.macSigningIdentity?.let { identity.set(it) }
                    }
                    provisioningProfile.set(project.file("$macDir/embedded.provisionprofile"))
                    runtimeProvisioningProfile.set(project.file("$macDir/runtime.provisionprofile"))
                }
            }
            windows {
                iconFile.set(rootProject.file("icons/app.ico"))
                dirChooser = true
                shortcut = true
                menu = true
            }
            linux {
                iconFile.set(rootProject.file("icons/app.png"))
            }
        }
    }
}

import io.sentry.kotlin.multiplatform.gradle.AppleDependencyProvider
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.jetbrainsComposeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.javafx) apply false
    // The Sentry Android Gradle plugin instruments the Android application and
    // uploads ProGuard mappings, so it lives in the :androidApp module now.
    alias(libs.plugins.sentryKmp)

    id("ooni.common")
}

val organization: String? by project

val config = Organization.fromKey(organization).config

// App version, single source of truth in the version catalog. Shared by the
// androidApp module (Android versionName/versionCode) and the desktop
// distribution below, which previously read android.defaultConfig.* — no
// longer available here now that the android {} block lives in :androidApp.
val appVersionName = libs.versions.app.versionName
    .get()
val appVersionCode = libs.versions.app.versionCode
    .get()
    .toInt()

// Active desktop distribution channel (resolved from -PdesktopDistribution).
// Drives whether JavaFX is on the classpath and which OoniWebView actual is
// compiled. The desktop packaging itself (compose.desktop) lives in :desktopApp.
val dist = distribution()

val javaFxParts = listOf("base", "graphics", "controls", "media", "web", "swing")
val javaFxVersion = "26.0.1"

if (dist.bundlesJavaFx) {
    apply(
        plugin = libs.plugins.javafx
            .get()
            .pluginId,
    )
}

kotlin {
    android {
        namespace = "org.ooni.probe.shared"
        compileSdk = libs.versions.android.compileSdk
            .get()
            .toInt()
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()

        // Launcher icons, notification_icon and provider_paths live in
        // commonMain/res (and per-org res copied there by the branding task);
        // merge them into the library's Android resources so the :androidApp
        // manifest can reference them.
        androidResources {
            enable = true
        }

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }

        // No Android host/device tests here: the Android entry points and the
        // instrumented UI tests that exercise them live in :androidApp now, and
        // shared (commonTest) logic is covered by :composeApp:desktopTest (JVM)
        // and the iOS test target.
    }

    // iosX64 is no longer supported by Compose Multiplatform
    // https://github.com/JetBrains/compose-multiplatform/pull/5514
    // iosX64()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "composeApp"
            isStatic = true
            binaryOption("bundleId", "composeApp")
        }
    }

    // iOS native dependencies, resolved with Swift Package Manager. Xcode links them into the
    // app through iosApp/KotlinMultiplatformLinkedPackage. After changing this block, the next
    // Xcode build regenerates that package and fails once; resolve packages in Xcode, build
    // again, and commit the regenerated files (see "Changing SwiftPM dependencies" in README.md).
    // Sentry Cocoa is added here by the Sentry plugin (see `sentryKmp` below).
    swiftPMDependencies {
        // Match DEPLOYMENT_TARGET in iosApp/Configuration/Config.xcconfig.
        iosMinimumDeploymentTarget = "15.0"

        // OONI probe engine (oonimkall) plus Tor, OpenSSL, libevent and zlib. Package.swift is the
        // one published with ooni/probe-cli's `rolling` release; the Swift app calls oonimkall
        // directly (IosOonimkallBridge). Refresh with `./gradlew :composeApp:updateIosOonimkallPackage`.
        localSwiftPackage(
            directory = rootProject.layout.projectDirectory.dir("iosApp/Packages/Oonimkall"),
            products = listOf("oonimkall"),
        )

        // Passport (ooniprobe-rs) UniFFI Swift bindings plus passport.xcframework, from the
        // ooniprobe-rs `rolling` release. The Swift app calls it directly (IosPassportBridge).
        // Refresh with `./gradlew :composeApp:updateIosPassportPackage` (see README.md).
        localSwiftPackage(
            directory = rootProject.layout.projectDirectory.dir("iosApp/Packages/Passport"),
            products = listOf("Passport"),
        )

        // App Store update prompts, used by the Swift app (iOSApp.swift).
        // 7.x requires iOS 17, so this stays on 6.x while DEPLOYMENT_TARGET is 15.0.
        swiftPackage(
            url = url("https://github.com/ArtSabintsev/Siren"),
            version = from("6.1.3"),
            products = listOf(product("Siren")),
        )

        // SQLite for the SQLDelight native driver, which links against sqlite3 symbols.
        swiftPackage(
            url = url("https://github.com/swiftlang/swift-toolchain-sqlite"),
            version = from("1.0.13"),
            products = listOf(product("SwiftToolchainCSQLite")),
        )
    }

    jvm("desktop")

    sourceSets {
        androidMain {
            // The flavor-specific Android code that STAYS here (the `Instrumentation`
            // expect/actual, plus FlavorConfig / AndroidUpdateMonitoring / AppReview
            // consumed by the moved entry points) is selected by task name, mirroring
            // the commonFullMain/commonFdroidMain selection. (Entry points, workers and
            // platform bridges now live in :androidApp.)
            kotlin.srcDir("src/${selectedAndroidFlavorDir()}/kotlin")
            dependencies {
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.compose.ui.tooling)
                implementation(libs.bundles.android)
                implementation(libs.bundles.mobile)
                implementation("${libs.jna.asProvider().get()}@aar")
                implementation("${libs.passport.android.get()}:@aar")
                // The Android engine bridge + flavor code live here (platform
                // implementations). fdroid/xperimental swap the oonimkall artifact;
                // full additionally pulls Play app-update/review for
                // AndroidUpdateMonitoring/AppReview.
                when {
                    isFdroidTaskRequested() ->
                        implementation("${libs.oonimkall.android.get()}:@aar")
                    isXperimentalTaskRequested() ->
                        implementation(files("libs/android-oonimkall.aar"))
                    else -> { // full
                        implementation(libs.bundles.full.android)
                        implementation("${libs.oonimkall.android.get()}:@aar")
                    }
                }
            }
        }
        commonMain {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.bundles.kotlin)
                implementation(libs.bundles.ui)
                implementation(libs.bundles.tooling)

                if (!isFdroidTaskRequested()) {
                    kotlin.srcDir("src/commonFullMain/kotlin")
                } else {
                    kotlin.srcDir("src/commonFdroidMain/kotlin")
                }
            }
            kotlin.srcDir(if (isDebugTaskRequested()) "src/commonMain/debug/kotlin" else "src/commonMain/release/kotlin")
            kotlin.srcDir("src/${config.folder}/kotlin")
            // SharedBuildConfig holds values needed by common (all-platform) code, so it is
            // generated into commonMain and compiles into every target (Android, iOS, desktop).
            kotlin.srcDir(tasks.named("generateSharedBuildConfig"))
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
            implementation(libs.bundles.mobile)
            implementation(libs.bundles.ios)
        }
        val desktopMain by getting {
            // The OoniWebView desktop actual is distribution-specific: JavaFX
            // builds get the embedded WebView, the Mac App Store build gets an
            // external-browser fallback that references no javafx.* symbols.
            if (dist.bundlesJavaFx) {
                kotlin.srcDir("src/desktopJavaFxMain/kotlin")
            } else {
                kotlin.srcDir("src/desktopNoJavaFxMain/kotlin")
            }
            // DesktopBuildConfig is desktop-only, so it is generated into desktopMain (not
            // commonMain); :desktopApp reaches it through implementation(project(":composeApp")).
            kotlin.srcDir(tasks.named("generateDesktopBuildConfig"))
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.bundles.desktop)
                implementation(
                    // Desktop oonimkall is a per-OS artifact (version carries the OS).
                    OperatingSystem.current().let { os ->
                        when {
                            os.isMacOsX -> libs.oonimkall.desktop.macos
                            os.isWindows -> libs.oonimkall.desktop.windows
                            os.isLinux -> libs.oonimkall.desktop.linux
                            else -> error("Unsupported OS for oonimkall desktop: $os")
                        }
                    },
                )
                implementation(
                    // Desktop passport is a per-OS artifact (module name carries the OS).
                    OperatingSystem.current().let { os ->
                        when {
                            os.isMacOsX -> libs.passport.macos
                            os.isWindows -> libs.passport.windows
                            os.isLinux -> libs.passport.linux
                            else -> error("Unsupported OS for passport desktop: $os")
                        }
                    },
                )

                if (dist.bundlesJavaFx) {
                    // The embedded WebView actual needs JavaFX. As JavaFX has
                    // platform-specific dependencies, we add them manually.
                    val fxSuffix = getJavaFxSuffix()
                    javaFxParts.forEach {
                        implementation("org.openjfx:javafx-$it:$javaFxVersion:$fxSuffix")
                    }
                }
            }
        }
        // Testing
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
        }
        val desktopTest by getting {
            // The desktop target keeps only the commonTest expect/actual fixture
            // actuals (in-memory DB / DataStore / SecureStorage); these need the
            // JVM SQLDelight + DataStore drivers on the test classpath.
            dependencies {
                implementation(libs.sqldelight.jvm)
                implementation(libs.androidx.datastore.preferences.core)
                implementation(libs.androidx.datastore.core.okio)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        all {
            languageSettings {
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlin.io.encoding.ExperimentalEncodingApi")
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlin.uuid.ExperimentalUuidApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlinx.coroutines.DelicateCoroutinesApi")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.coroutines.FlowPreview")
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                optIn("androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi")
                optIn("androidx.compose.ui.test.ExperimentalTestApi")
                optIn("androidx.compose.ui.ExperimentalComposeUiApi")
            }
        }
    }

    compilerOptions {
        // Common compiler options applied to all Kotlin source sets
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

if (dist.bundlesJavaFx) {
    configure<org.openjfx.gradle.JavaFXOptions> {
        version = javaFxVersion
        modules = javaFxParts.map { "javafx.$it" }
    }
}

tasks.withType<Test>().configureEach {
    filter {
        // Only treat *Test classes as tests; skip commonTest/desktopTest helpers
        // (factories, Kotlin file-facade *Kt classes, $Companion, etc.) which JUnit 4's
        // class-file scanner would otherwise try to run and report as InvalidTestClassError.
        includeTestsMatching("*Test")
        isFailOnNoMatchingTests = false
    }
    testLogging {
        // Surface test stdout (e.g. the CountNotViewedBenchmarkTest timing table) on the console.
        showStandardStreams = true
    }
}

// The KMP library Android variant only reads src/androidMain/res by default.
// The launcher icons / notification_icon land in src/commonMain/res (copied
// there from src/<org>/res by copyBrandingToCommonResources), so add that
// directory to the Android resources for every variant.
androidComponents {
    onVariants { variant ->
        variant.sources.res?.addStaticSourceDirectory("src/commonMain/res")
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName = "org.ooni.probe"
            schemaOutputDirectory = file("src/commonMain/sqldelight/databases")
            verifyMigrations = true
        }
    }
}

ktlint {
    filter {
        exclude {
            val p = it.file.path.replace('\\', '/')
            p.contains("/build/generated/")
        }
        include("**/kotlin/**")
    }
    additionalEditorconfig.put("ktlint_function_naming_ignore_when_annotated_with", "Composable")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "ooniprobe.composeapp.generated.resources"
    generateResClass = always
}

version = appVersionName

// Sentry

// The Sentry Gradle plugin and its `sentry { }` configuration live in the
// :androidApp module now (they instrument the Android application and upload
// ProGuard mappings). These exclusions still apply here because they act on the
// Sentry Kotlin Multiplatform SDK's Android artifact, resolved via commonFullMain.
configurations.all {
    exclude(group = "io.sentry", module = "sentry-android-ndk")
    exclude(group = "io.sentry", module = "sentry-android-replay")
}

sentryKmp {
    autoInstall {
        // fdroid ships no crash reporting; only auto-install the KMP SDK dependency
        // into commonMain for full/xperimental builds.
        commonMain {
            enabled.set(!isFdroidTaskRequested())
        }
        // Sentry Cocoa 9 (KMP SDK 0.28.0+) no longer supports CocoaPods. Install it through
        // Kotlin's official SwiftPM import; Xcode links it via iosApp/KotlinMultiplatformLinkedPackage.
        // SWIFT_PM (instead of the AUTO default) fails the build rather than silently skipping
        // the install if the Kotlin CocoaPods plugin is ever applied again.
        // https://docs.sentry.io/platforms/kotlin/guides/kotlin-multiplatform/configuration/gradle/#migrate-from-cocoapods
        apple.provider.set(AppleDependencyProvider.SWIFT_PM)
    }
}

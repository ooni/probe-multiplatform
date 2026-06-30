import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.jetbrainsComposeCompiler)
    alias(libs.plugins.cocoapods)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.javafx) apply false
    // The Sentry Android Gradle plugin instruments the Android application and
    // uploads ProGuard mappings, so it lives in the :androidApp module now.

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
    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    cocoapods {
        ios.deploymentTarget = "14.0"

        version = "1.0.0"
        summary = "Compose App"
        homepage = "https://github.com/ooni/probe-multiplatform"

        framework {
            baseName = "composeApp"
            isStatic = true
            binaryOption("bundleId", "composeApp")
        }

        // See https://github.com/getsentry/sentry-kotlin-multiplatform?tab=readme-ov-file#cocoa-sdk-version-compatibility-table
        pod("Sentry") {
            version = "8.58.2"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        podfile = project.file("../iosApp/Podfile")
    }

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
                implementation(libs.bundles.android)
                implementation(libs.bundles.mobile)
                implementation("net.java.dev.jna:jna:5.19.1@aar")
                implementation("org.ooni:passport-android:0.1.3:@aar")
                // The Android engine bridge + flavor code live here (platform
                // implementations). fdroid/xperimental swap the oonimkall artifact;
                // full additionally pulls Play app-update/review for
                // AndroidUpdateMonitoring/AppReview.
                when {
                    isFdroidTaskRequested() ->
                        implementation("org.ooni:oonimkall:3.29.0-android:@aar")
                    isXperimentalTaskRequested() ->
                        implementation(files("libs/android-oonimkall.aar"))
                    else -> { // full
                        implementation(libs.bundles.full.android)
                        implementation("org.ooni:oonimkall:3.29.0-android:@aar")
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
                    implementation(libs.bundles.full)
                    kotlin.srcDir("src/commonFullMain/kotlin")
                } else {
                    kotlin.srcDir("src/commonFdroidMain/kotlin")
                }
            }
            kotlin.srcDir(if (isDebugTaskRequested()) "src/commonMain/debug/kotlin" else "src/commonMain/release/kotlin")
            kotlin.srcDir("src/${config.folder}/kotlin")
            // DesktopBuildConfig is generated here (not in :desktopApp) so it compiles into
            // the desktop jvm artifact and is resolvable from both :composeApp's desktop code
            // (Distribution.kt, desktopTest fixtures) and :desktopApp via its project dependency.
            kotlin.srcDir(tasks.named("generateDesktopBuildConfig"))
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
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.bundles.desktop)
                implementation("org.ooni:oonimkall:c52ce3b5-${oonimkallVersionSuffix()}")
                implementation("org.ooni:passport-${passportDependencySuffix()}:0.1.3")

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

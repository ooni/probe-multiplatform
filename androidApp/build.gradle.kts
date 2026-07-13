import com.android.build.api.variant.FilterConfiguration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.jetbrainsComposeCompiler)
    alias(libs.plugins.sentry)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

// The instrumented tests (src/androidTest) and the shared org.ooni.testing fixtures symlinked from
// :composeApp's commonTest rely on the experimental APIs that :composeApp opts into module-wide via
// languageSettings. This plain Android module has no languageSettings block, so opt in here —
// scoped to the androidTest compilation, since some markers (e.g. Compose ui-test) are only on that
// classpath. (Omits the iOS-only kotlinx.cinterop.ExperimentalForeignApi and the unused
// material3 window-size-class marker.)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (name.contains("AndroidTest")) {
        compilerOptions.optIn.addAll(
            "kotlin.ExperimentalStdlibApi",
            "kotlin.io.encoding.ExperimentalEncodingApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlinx.coroutines.DelicateCoroutinesApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
            "org.jetbrains.compose.resources.ExperimentalResourceApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.ui.test.ExperimentalTestApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
        )
    }
}

val organization: String? by project

val config = Organization.fromKey(organization).config

android {
    namespace = "org.ooni.probe"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()

    defaultConfig {
        applicationId = config.appId
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
        targetSdk = libs.versions.android.targetSdk
            .get()
            .toInt()
        versionCode = libs.versions.app.versionCode
            .get()
            .toInt() // Always increment by 10. See fdroid flavor below
        versionName = libs.versions.app.versionName.get()
        resValue("string", "app_name", config.appName)
        resValue("string", "ooni_run_enabled", config.supportsOoniRun.toString())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }
    androidResources {
        localeFilters += config.supportedLanguages
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // Unable to strip the following libraries, packaging them as they are:
        jniLibs.keepDebugSymbols.addAll(
            listOf(
                "**/libandroidx.graphics.path.so",
                "**/libdatastore_shared_counter.so",
                "**/libgojni.so",
            ),
        )
    }

    signingConfigs {
        create("release") {
            if (System.getenv("ANDROID_KEYSTORE_FILE") != null &&
                System.getenv("ANDROID_KEYSTORE_PASSWORD") != null &&
                System.getenv("ANDROID_KEY_ALIAS") != null &&
                System.getenv("ANDROID_KEY_PASSWORD") != null
            ) {
                storeFile = file(System.getenv("ANDROID_KEYSTORE_FILE"))
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            resValue("string", "run_v2_domain", "run.test.ooni.org")
            sourceSets["debug"].manifest.srcFile("src/debug/AndroidManifest.xml")
        }
        getByName("release") {
            resValue("string", "run_v2_domain", "run.ooni.org")
            signingConfig = if (System.getenv("ANDROID_KEYSTORE_FILE") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    buildFeatures {
        compose = true
        resValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
        isCoreLibraryDesugaringEnabled = true
    }
    productFlavors {
        create("full") {
            dimension = "license"
        }
        create("xperimental") {
            dimension = "license"
        }
        create("fdroid") {
            dimension = "license"
            // Our APK is too large and F-Droid asked for a split by ABI
            splits {
                abi {
                    // Detect app bundle and conditionally disable split abis
                    // This is needed due to a "Sequence contains more than one matching element" error
                    // present since AGP 8.9.0, for more info see:
                    // https://issuetracker.google.com/issues/402800800

                    // AppBundle tasks usually contain "bundle" in their name
                    val isBuildingBundle = gradle.startParameter.taskNames.any { it.lowercase().contains("bundle") }

                    // Disable split abis when building appBundle
                    isEnable = !isBuildingBundle

                    reset()
                    // Specifies a list of ABIs supported by probe-engine.
                    include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
                    // Specifies that you don't want to also generate a universal APK that includes all ABIs.
                    isUniversalApk = true
                }
            }

            // Map for the version code that gives each ABI a value.
            val abiCodes =
                mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

            androidComponents {
                onVariants { variant ->
                    variant.outputs.forEach { output ->
                        val name = output.filters.find { it.filterType == FilterConfiguration.FilterType.ABI }?.identifier

                        val baseAbiCode = abiCodes[name]

                        if (baseAbiCode != null) {
                            output.versionCode.set(
                                baseAbiCode + (output.versionCode.get() ?: 0),
                            )
                        }
                    }
                }
            }
        }
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Keeps dependency metadata when building Android App Bundles.
        includeInBundle = true
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
    lint {
        warningsAsErrors = true
        disable += listOf(
            "AndroidGradlePluginVersion",
            "NullSafeMutableLiveData",
            "ObsoleteLintCustomCheck",
            "Aligned16KB",
            "UseTomlInstead", // We are using this until the classifier issue is resolved in https://github.com/ooni/probe-cli/issues/1739
        )
        lintConfig = file("lint.xml")
    }
    flavorDimensions += "license"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":composeApp"))

    // Compose — MainActivity hosts setContent { App() }
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.components)
    implementation(libs.compose.ui.tooling.preview)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(libs.navigation)

    // Android libraries used by the moved entry points / workers
    implementation(libs.bundles.android) // activity, fragment, webkit, work, sqldelight-android, appcompat, ktor-android
    implementation(libs.kermit)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.androidx.datastore.preferences.core)
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    coreLibraryDesugaring(libs.android.desugar.jdk)

    // Instrumented UI tests (src/androidTest, run on Firebase Test Lab). The runner
    // (androidx.test:runner) lives in the android-instrumented-test bundle — without it the
    // testInstrumentationRunner above is missing at runtime (NO_TEST_RUNNER_CLASS).
    androidTestImplementation(libs.bundles.android.test)
    androidTestImplementation(libs.bundles.android.instrumented.test)
    androidTestImplementation(libs.kotlinx.coroutines.test) // runTest {} — not in either bundle
    // The shared org.ooni.testing fixtures (symlinked from :composeApp commonTest) use
    // kotlinx-datetime directly; :composeApp pulls it as implementation so it isn't on our classpath.
    androidTestImplementation(libs.kotlin.datetime)
    androidTestUtil(libs.android.orchestrator)
}

// Sentry

sentry {
    val hasSentryToken = !System.getenv("SENTRY_AUTH_TOKEN").isNullOrEmpty()
    debug = false
    org = "ooni"
    projectName = "probe-multiplatform-android"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
    includeProguardMapping = hasSentryToken
    autoUploadProguardMapping = hasSentryToken
    uploadNativeSymbols = hasSentryToken
    autoUploadNativeSymbols = hasSentryToken
    includeSourceContext = false
    autoInstallation {
        enabled = false
    }
    telemetry = false
    ignoredBuildTypes = listOf("debug")
    ignoredFlavors = listOf("fdroid", "xperimental")
}

// Remove certain Sentry components
configurations.all {
    exclude(group = "io.sentry", module = "sentry-android-ndk")
    exclude(group = "io.sentry", module = "sentry-android-replay")
}

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.jetbrainsComposeCompiler)
    alias(libs.plugins.cocoapods)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
}

val flavor: String by project

val appConfig =
    mapOf(
        "dw" to
            AppConfig(
                appId = "org.dw.probe",
                appName = "News Media Scan",
                srcRoot = "src/dwMain/kotlin",
                resRoot = "src/dwMain/resources",
            ),
        "ooni" to
            AppConfig(
                appId = "org.ooni.probe",
                appName = "OONI Probe",
                srcRoot = "src/ooniMain/kotlin",
                resRoot = "src/ooniMain/resources",
            ),
    )

val config = appConfig[flavor] ?: appConfig["ooni"]!!

println("The current build flavor is set to $flavor with app id set to ${config.appId}.")

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        ios.deploymentTarget = "9.0"

        version = "1.0"
        summary = "Compose App"
        homepage = "https://github.com/ooni/probe-multiplatform"

        framework {
            baseName = "composeApp"
            isStatic = true
            binaryOption("bundleId", "composeApp")
        }

        podfile = project.file("../iosApp/Podfile")
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.android.oonimkall)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlin.serialization)
            implementation(libs.bundles.tooling)

            getByName("commonMain") {
                kotlin.srcDir(config.srcRoot)
            }
        }
        all {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        // Common compiler options applied to all Kotlin source sets
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    composeCompiler {
        enableStrongSkippingMode = true
    }
}

compose.resources {
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = provider { layout.projectDirectory.dir(config.resRoot) },
    )
}

android {
    namespace = "org.ooni.probe"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = config.appId
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        resValue("string", "app_name", config.appName)
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
    android {
        lint {
            warningsAsErrors = true
            disable += "AndroidGradlePluginVersion"
        }
    }
}

ktlint {
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
    additionalEditorconfig.put("ktlint_function_naming_ignore_when_annotated_with", "Composable")
}

data class AppConfig(
    val appId: String,
    val appName: String,
    val srcRoot: String,
    val resRoot: String,
)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.sqldelight)
    id("ooni.common")
}

val organization: String? by project
val config = Organization.fromKey(organization).config

val appVersionName = libs.versions.app.versionName
    .get()
val appVersionCode = libs.versions.app.versionCode
    .get()
    .toInt()

kotlin {
    jvmToolchain(25)

    android {
        namespace = "org.ooni.probe.core"
        compileSdk = libs.versions.android.compileSdk
            .get()
            .toInt()
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.serialization.json)
            implementation(libs.kotlin.serialization.xml)
            implementation(libs.kotlin.datetime)
            implementation(libs.kermit)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.androidx.datastore.core.okio)
            implementation(libs.okio)
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.sqldelight.jvm)
                implementation(libs.androidx.datastore.preferences.core)
                implementation(libs.androidx.datastore.core.okio)
                implementation(libs.jna)
                implementation(libs.jna.platform)
                // api: cliApp's DesktopCliGeoIpGateway/CliDesktopPassportBridge call these native
                // bindings directly, so they must be visible on cliApp's compile classpath too.
                api(
                    OperatingSystem.current().let { os ->
                        when {
                            os.isMacOsX -> libs.oonimkall.desktop.macos
                            os.isWindows -> libs.oonimkall.desktop.windows
                            os.isLinux -> libs.oonimkall.desktop.linux
                            else -> error("Unsupported OS for oonimkall desktop: $os")
                        }
                    },
                )
                api(
                    OperatingSystem.current().let { os ->
                        when {
                            os.isMacOsX -> libs.passport.macos
                            os.isWindows -> libs.passport.windows
                            os.isLinux -> libs.passport.linux
                            else -> error("Unsupported OS for passport desktop: $os")
                        }
                    },
                )
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopTest by getting {
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
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

version = appVersionName

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

tasks.withType<Test>().configureEach {
    filter {
        includeTestsMatching("*Test")
        isFailOnNoMatchingTests = false
    }
    testLogging {
        showStandardStreams = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addStaticSourceDirectory("src/commonMain/res")
    }
}

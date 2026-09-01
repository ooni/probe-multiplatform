import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.graalvmNative)
    application
    id("ooni.common")
}

val appVersionName = libs.versions.app.versionName.get()
val appVersionCode = libs.versions.app.versionCode.get().toInt()

val generateCliBuildConfig = tasks.register("generateCliBuildConfig") {
    val outputDir = layout.buildDirectory.dir("generated/cliBuildConfig/kotlin")
    inputs.property("versionName", appVersionName)
    inputs.property("versionCode", appVersionCode)
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("org/ooni/probe/cli")
        dir.mkdirs()
        dir.resolve("CliBuildConfig.kt").writeText(
            """
            |package org.ooni.probe.cli
            |
            |object CliBuildConfig {
            |    const val VERSION_NAME = "$appVersionName"
            |    const val VERSION_CODE = $appVersionCode
            |}
            """.trimMargin(),
        )
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
    sourceSets {
        main {
            kotlin.srcDir(generateCliBuildConfig)
        }
    }
}

dependencies {
    implementation(project(":probeCore"))
    // DesktopNetworkTypeFinder (real network type detection instead of an "unknown" stub).
    implementation(project(":desktopShared"))
    implementation(libs.clikt)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.datetime)
    // The Cli*Gateway desktop implementations (moved from probeCore) construct SqlDriver/Database,
    // DataStore preferences, Json, and okio.FileSystem directly.
    implementation(libs.sqldelight.jvm)
    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.okio)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.coroutines.test)
}

application {
    mainClass = "org.ooni.probe.cli.MainKt"
    applicationName = "ooniprobe"
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("ooniprobe")
            mainClass.set("org.ooni.probe.cli.MainKt")
            buildArgs.add("--no-fallback")
            // Enable JNI and other features
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("--initialize-at-build-time=co.touchlab.kermit")
            buildArgs.add("--initialize-at-build-time=androidx.datastore.preferences.protobuf")
        }
    }
    metadataRepository {
        enabled.set(true)
    }
    agent {
        enabled.set(true)
    }
}

version = appVersionName

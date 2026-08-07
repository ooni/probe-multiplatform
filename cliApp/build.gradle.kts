import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
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
    implementation(libs.clikt)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.datetime)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.androidx.datastore.preferences.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.serialization.json)
}

application {
    mainClass = "org.ooni.probe.cli.MainKt"
    applicationName = "ooniprobe"
}

version = appVersionName

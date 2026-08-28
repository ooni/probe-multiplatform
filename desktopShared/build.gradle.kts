import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    id("ooni.common")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

dependencies {
    // Desktop-only shared code consumed by both :composeApp (desktopMain)/:desktopApp and :cliApp.
    // Depends only on :probeCore - no Compose Multiplatform, no composeApp/desktopApp globals.
    implementation(project(":probeCore"))
    implementation(libs.kermit)

    testImplementation(kotlin("test-junit"))
}

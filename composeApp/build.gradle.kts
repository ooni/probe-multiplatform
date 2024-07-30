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

val organization: String? by project

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

val config = appConfig[organization] ?: appConfig["ooni"]!!

println("The current build flavor is set to $organization with app id set to ${config.appId}.")

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
            implementation(compose.preview)
            implementation(libs.android.oonimkall)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlin.serialization)
            implementation(libs.bundles.ui)
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
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
    android {
        lint {
            warningsAsErrors = true
            disable += listOf("AndroidGradlePluginVersion", "ObsoleteLintCustomCheck")
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

tasks.register("copyBrandingToCommonResources") {
    doLast {
        val projectDir = project.projectDir.absolutePath

        val destinationFile = File(projectDir, "src/commonMain/composeResources")

        val sourceFile = File(projectDir, config.resRoot)

        copyRecursive(sourceFile, destinationFile)
    }
}

tasks.register("cleanCopiedCommonResourcesToFlavor") {
    doLast {
        val projectDir = project.projectDir.absolutePath

        val destinationFile = File(projectDir, "src/commonMain/composeResources")
        destinationFile.listFiles()?.forEach { folder ->
            folder.listFiles()?.forEach { file ->
                if (file.name == ".gitignore") {
                    file.readText().lines().forEach { line ->
                        if (line.isNotEmpty()) {
                            println("Removing $line")
                            File(folder, line).deleteRecursively()
                        }
                    }.also {
                        file.delete()
                    }
                }
            }
        }
    }
}

/**
 * Configure the prepareComposeResourcesTaskForCommonMain task to depend on the copyBrandingToCommonResources task.
 * This will ensure that the common resources are copied to the correct location before the task is executed.
 *
 * NOTE: Current limitation is that multiple resources directories are not supported.
 */
tasks.named("preBuild").configure {
    dependsOn("copyBrandingToCommonResources")
}

tasks.named("clean").configure {
    dependsOn("copyBrandingToCommonResources")
}

tasks.named("clean").configure {
    dependsOn("cleanCopiedCommonResourcesToFlavor")
}

data class AppConfig(
    val appId: String,
    val appName: String,
    val srcRoot: String,
    val resRoot: String,
)

/**
 * Ignore the copied file if it is not already ignored.
 *
 * @param filePath The path to the file to ignore.
 * @param lineToAdd The line to add to the file.
 */
fun ignoreCopiedFileIfNotIgnored(
    filePath: String,
    lineToAdd: String,
) {
    val file = File(filePath)

    if (!file.exists()) {
        file.createNewFile()
    }

    val fileContents = file.readText()

    if (!fileContents.contains(lineToAdd)) {
        file.appendText("\n$lineToAdd")
    }
}

/**
 * Copy files from one directory to another.
 *
 * @param from The source directory.
 * @param to The destination directory.
 */
fun copyRecursive(
    from: File,
    to: File,
) {
    if (!from.exists()) {
        println("Source directory does not exist: $from")
        return
    }
    from.listFiles()?.forEach { file ->
        if (file.name != ".DS_Store") {
            if (file.isDirectory) {
                val newDir = File(to, file.name)
                newDir.mkdir()
                copyRecursive(file, newDir)
            } else {
                val destinationFile = File(to, file.name)
                if (destinationFile.exists()) {
                    println("Overwriting $destinationFile")
                    destinationFile.delete()
                }
                if (!destinationFile.parentFile.exists()) {
                    destinationFile.parentFile.mkdirs()
                }
                file.copyTo(destinationFile).also {
                    println("Ignoring ${it.name}")
                    ignoreCopiedFileIfNotIgnored(
                        to.absolutePath + "/.gitignore",
                        it.name,
                    )
                }
            }
        }
    }
}

tasks.register("runDebug", Exec::class) {
    dependsOn("clean", "uninstallDebug", "installDebug")
    commandLine(
        "adb",
        "shell",
        "am",
        "start",
        "-n",
        "${config.appId}.debug/org.ooni.probe.MainActivity",
    )
}

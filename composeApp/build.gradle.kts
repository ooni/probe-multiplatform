import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.jetbrainsComposeCompiler)
    alias(libs.plugins.cocoapods)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.sqldelight)
}

val organization: String? by project

val appConfig = mapOf(
    "dw" to AppConfig(
        appId = "com.dw.ooniprobe",
        appName = "News Media Scan",
        folder = "dwMain",
        supportsOoniRun = false,
        supportedLanguages = listOf(
            "de", "es", "fr", "pt-rBR", "ru", "tr",
        ),
    ),
    "ooni" to AppConfig(
        appId = "org.openobservatory.ooniprobe",
        appName = "OONI Probe",
        folder = "ooniMain",
        supportsOoniRun = true,
        supportedLanguages = listOf(
            "ar", "ca", "de", "el", "es", "fa", "fr", "hi", "id", "is", "it", "my", "nl", "pt-rBR",
            "ro", "ru", "sk", "sq", "sw", "th", "tr", "vi", "zh-rCN", "zh-rTW",
        ),
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
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant {
            // This makes instrumented tests depend on commonTest and androidUnitTest sources
            sourceSetTree.set(KotlinSourceSetTree.test)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

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

        pod("Sentry") {
            version = "~> 8.38.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        podfile = project.file("../iosApp/Podfile")
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.bundles.android)
        }
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
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
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        androidUnitTest.dependencies {
            implementation(kotlin("test-junit"))
            implementation(libs.bundles.android.test)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.bundles.android.instrumented.test)
        }
        all {
            languageSettings {
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlin.io.encoding.ExperimentalEncodingApi")
                optIn("kotlinx.cinterop.BetaInteropApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.coroutines.FlowPreview")
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                optIn("androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi")
                optIn("androidx.compose.ui.test.ExperimentalTestApi")
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        // Common compiler options applied to all Kotlin source sets
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "org.ooni.probe"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = config.appId
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 131
        versionName = "5.0.2"
        resValue("string", "app_name", config.appName)
        resValue("string", "ooni_run_enabled", config.supportsOoniRun.toString())
        resValue(
            "string",
            "supported_languages",
            config.supportedLanguages.joinToString(separator = ","),
        )
        resourceConfigurations += config.supportedLanguages
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            resValue("string", "run_v2_domain", "run.test.ooni.org")
            sourceSets["debug"].manifest.srcFile("src/androidDebug/AndroidManifest.xml")
        }
        getByName("release") {
            isMinifyEnabled = false
            resValue("string", "run_v2_domain", "run.ooni.org")
            signingConfig = signingConfigs.getByName("debug")
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
    sourceSets["main"].resources.setSrcDirs(
        listOf(
            "src/androidMain/resources",
            "src/commonMain/resources",
        ),
    )
    sourceSets["main"].res.setSrcDirs(
        listOf(
            "src/androidMain/res",
            "src/commonMain/res",
        ),
    )
    productFlavors {
        create("full") {
            dimension = "license"
        }
        create("fdroid") {
            dimension = "license"
        }
    }
    dependencies {
        debugImplementation(compose.uiTooling)
        "fullImplementation"(libs.bundles.full.android)
        androidTestUtil(libs.android.orchestrator)
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
    lint {
        warningsAsErrors = true
        disable += listOf("AndroidGradlePluginVersion", "ObsoleteLintCustomCheck")
        lintConfig = file("lint.xml")
    }
    flavorDimensions += "license"
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
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
    additionalEditorconfig.put("ktlint_function_naming_ignore_when_annotated_with", "Composable")
}

// Fix to exclude sqldelight generated files
tasks {
    listOf(
        runKtlintFormatOverCommonMainSourceSet,
        runKtlintCheckOverCommonMainSourceSet,
    ).forEach {
        it {
            setSource(
                kotlin.sourceSets.commonMain.map {
                    it.kotlin.filter { file -> !file.absolutePath.contains("generated") }
                },
            )
        }
    }
}

fun isFdroidTaskRequested(): Boolean {
    return gradle.startParameter.taskRequests.flatMap { it.args }.any { it.contains("Fdroid") }
}

fun isDebugTaskRequested(): Boolean {
    return gradle.startParameter.taskRequests.flatMap { it.args }.any { it.contains("Debug") }
}

tasks.register("copyBrandingToCommonResources") {
    doLast {
        val projectDir = project.projectDir.absolutePath
        copyRecursive(
            from = File(projectDir, "src/${config.folder}/res"),
            to = File(projectDir, "src/commonMain/res"),
        )
        copyRecursive(
            from = File(projectDir, "src/${config.folder}/resources"),
            to = File(projectDir, "src/commonMain/resources"),
        )
        copyRecursive(
            from = File(projectDir, "src/${config.folder}/composeResources"),
            to = File(projectDir, "src/commonMain/composeResources"),
        )
    }
}

tasks.register("cleanCopiedCommonResourcesToFlavor") {
    doLast {
        val projectDir = project.projectDir.absolutePath

        fun deleteFilesFromGitIgnore(folderPath: String) {
            val destinationFile = File(projectDir, folderPath)
            destinationFile.listFiles()?.forEach { folder ->
                folder.listFiles()?.forEach { file ->
                    if (file.name == ".gitignore") {
                        file
                            .readText()
                            .lines()
                            .forEach { line ->
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
        deleteFilesFromGitIgnore("src/commonMain/res")
        deleteFilesFromGitIgnore("src/commonMain/resources")
        deleteFilesFromGitIgnore("src/commonMain/composeResources")
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
    val folder: String,
    val supportsOoniRun: Boolean = false,
    val supportedLanguages: List<String>,
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
    dependsOn("clean", "installFullDebug")
    commandLine(
        "adb",
        "shell",
        "am",
        "start",
        "-n",
        "${config.appId}.dev/org.ooni.probe.MainActivity",
    )
}

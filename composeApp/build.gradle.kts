import com.android.build.api.variant.FilterConfiguration.FilterType.ABI
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
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
    alias(libs.plugins.conveyor)
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

    jvm("desktop")
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.JETBRAINS)
    }

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
            version = "~> 8.45.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        podfile = project.file("../iosApp/Podfile")
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.bundles.android)
            implementation(libs.bundles.mobile)
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
            implementation(libs.bundles.mobile)
        }
        val desktopMain by getting {
            dependencies {
                implementation(files("./src/desktopMain/libs/oonimkall.jar"))
                implementation(compose.desktop.currentOs)
                implementation(libs.bundles.desktop)
            }
        }
        // Testing
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
        versionCode = 190 // Always increment by 10. See fdroid flavor below
        versionName = "5.0.5"
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
            sourceSets["debug"].manifest.srcFile("src/androidDebug/AndroidManifest.xml")
        }
        getByName("release") {
            isMinifyEnabled = false
            resValue("string", "run_v2_domain", "run.ooni.org")
            signingConfig = if (System.getenv("ANDROID_KEYSTORE_FILE") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
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
            // Our APK is too large and F-Droid asked for a split by ABI
            splits {
                abi {
                    isEnable = true
                    // Resets the list of ABIs for Gradle to create APKs for to none.
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
                        val name = output.filters.find { it.filterType == ABI }?.identifier

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
    dependencies {
        coreLibraryDesugaring(libs.android.desugar.jdk)
        debugImplementation(compose.uiTooling)
        "fullImplementation"(libs.bundles.full.android)
        androidTestUtil(libs.android.orchestrator)
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

// Desktop

compose.desktop {
    application {
        mainClass = "org.ooni.probe.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ooni-probe"
            packageVersion = android.defaultConfig.versionName

            modules("java.sql", "jdk.unsupported")

            macOS {
                minimumSystemVersion = "10.15.0"
            }
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "ooniprobe.composeapp.generated.resources"
    generateResClass = always
}

// Conveyor

// region Work around temporary Compose bugs.
configurations.all {
    attributes {
        // https://github.com/JetBrains/compose-jb/issues/1404#issuecomment-1146894731
        attribute(Attribute.of("ui", String::class.java), "awt")
    }
}

version = android.defaultConfig.versionName ?: ""

dependencies {
    debugImplementation(compose.uiTooling)

    // Use the configurations created by the Conveyor plugin to tell Gradle/Conveyor where to find the artifacts for each platform.
    linuxAmd64(compose.desktop.linux_x64)
    macAmd64(compose.desktop.macos_x64)
    macAarch64(compose.desktop.macos_arm64)
    windowsAmd64(compose.desktop.windows_x64)
}

// Resources

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

tasks.register("copyBrandingToCommonResources") {
    doLast {
        val projectDir = project.projectDir.absolutePath
        copyRecursive(
            from = File(projectDir, "src/${config.folder}/res"),
            to = File(projectDir, "src/commonMain/res"),
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

// Helpers

data class AppConfig(
    val appId: String,
    val appName: String,
    val folder: String,
    val supportsOoniRun: Boolean = false,
    val supportedLanguages: List<String>,
)

fun isFdroidTaskRequested(): Boolean {
    return gradle.startParameter.taskRequests.flatMap { it.args }.any { it.contains("Fdroid") }
}

fun isDebugTaskRequested(): Boolean {
    return gradle.startParameter.taskRequests.flatMap { it.args }.any { it.contains("Debug") }
}

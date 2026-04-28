import com.android.build.api.variant.FilterConfiguration
import org.gradle.api.GradleException
import org.gradle.api.tasks.Sync
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import java.time.LocalDate
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.jetbrainsComposeCompiler)
    alias(libs.plugins.cocoapods)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.javafx)
    alias(libs.plugins.sentry)

    id("ooni.common")
}

val organization: String? by project

val config = Organization.fromKey(organization).config

val javaFxParts = listOf("base", "graphics", "controls", "media", "web", "swing")
val javaFxVersion = "25.0.2"

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
            version = "8.57.3"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        podfile = project.file("../iosApp/Podfile")
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.bundles.android)
            implementation(libs.bundles.mobile)
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
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
            implementation(libs.bundles.mobile)
            implementation(libs.bundles.ios)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.bundles.desktop)

                // As JavaFX have platform-specific dependencies, we need to add them manually
                val fxSuffix = getJavaFxSuffix()
                javaFxParts.forEach {
                    implementation("org.openjfx:javafx-$it:$javaFxVersion:$fxSuffix")
                }

                implementation("org.ooni:oonimkall:c52ce3b5-${oonimkallVersionSuffix()}")
            }
        }
        // Testing
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
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
        // Switch to future default rule: https://youtrack.jetbrains.com/issue/KT-73255
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

javafx {
    version = javaFxVersion
    modules = javaFxParts.map { "javafx.$it" }
}

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
        versionCode = 286 // Always increment by 10. See fdroid flavor below
        versionName = "6.0.1"
        resValue("string", "app_name", config.appName)
        resValue("string", "ooni_run_enabled", config.supportsOoniRun.toString())
        resValue(
            "string",
            "supported_languages",
            config.supportedLanguages.joinToString(separator = ","),
        )
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
            sourceSets["debug"].manifest.srcFile("src/androidDebug/AndroidManifest.xml")
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
    dependencies {
        coreLibraryDesugaring(libs.android.desugar.jdk)
        debugImplementation(libs.compose.ui.tooling)
        debugImplementation(libs.androidx.ui.tooling.preview)
        "fullImplementation"(libs.bundles.full.android)
        "fullImplementation"("org.ooni:oonimkall:3.29.0-android:@aar")
        "fdroidImplementation"("org.ooni:oonimkall:3.29.0-android:@aar")
        "xperimentalImplementation"(files("libs/android-oonimkall.aar"))
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

// Desktop

val dist = distribution()

val desktopResourcesDir = project.layout.projectDirectory.dir("src/desktopMain/resources/")
val preparedResourcesDir = layout.buildDirectory.dir("tmp/desktop-resources-${dist.name.lowercase()}")
val macosNativeLibrariesDir = layout.buildDirectory.dir("tmp/macos-native-libs")

// Extract bundled native libraries (JNA dispatcher, sqlite-jdbc, gojni) from
// their runtime jars so they can be staged inside the macOS .app and signed
// with our Team ID. Mac App Store builds run with the app sandbox + hardened
// runtime, which enforces library validation even when
// `com.apple.security.cs.disable-library-validation` is set — so the default
// behaviour of these libraries (unpacking dylibs into ~/Library/Caches/... at
// first use) is rejected by Gatekeeper with "Apple could not verify ... is
// free of malware". Bundling pre-signed copies inside Contents/app/resources/
// avoids the on-disk extraction entirely.
val extractMacOsNativeLibraries = tasks.register("extractMacOsNativeLibraries") {
    val runtimeClasspath = configurations.named("desktopRuntimeClasspath")
    val outDirProvider = macosNativeLibrariesDir
    inputs.files(runtimeClasspath)
    outputs.dir(outDirProvider)
    doLast {
        val out = outDirProvider.get().asFile
        out.deleteRecursively()
        out.mkdirs()

        // (jarNamePattern, listOf((entryInJar, outRelativePath)), required)
        val plans: List<Triple<Regex, List<Pair<String, String>>, Boolean>> = listOf(
            // JNA dispatcher — required on macOS for the JNA-based platform
            // helpers (autolaunch, dark mode detector, etc).
            Triple(
                Regex("""^jna-\d.*\.jar$"""),
                listOf(
                    "com/sun/jna/darwin-aarch64/libjnidispatch.jnilib" to "jna/darwin-aarch64/libjnidispatch.jnilib",
                    "com/sun/jna/darwin-x86-64/libjnidispatch.jnilib" to "jna/darwin-x86-64/libjnidispatch.jnilib",
                    "com/sun/jna/darwin/libjnidispatch.jnilib" to "jna/darwin/libjnidispatch.jnilib",
                ),
                true,
            ),
            // sqlite-jdbc native — required for the SQLDelight desktop driver.
            Triple(
                Regex("""^sqlite-jdbc-.*\.jar$"""),
                listOf(
                    "org/sqlite/native/Mac/aarch64/libsqlitejdbc.dylib" to "sqlite/darwin-aarch64/libsqlitejdbc.dylib",
                    "org/sqlite/native/Mac/x86_64/libsqlitejdbc.dylib" to "sqlite/darwin-x86-64/libsqlitejdbc.dylib",
                ),
                true,
            ),
            // oonimkall (gojni) — only present on the macOS classifier of
            // oonimkall, so this jar is only on the classpath for desktop
            // macOS builds. Skip silently if not found (e.g. when this task
            // is invoked from a non-macOS host).
            Triple(
                Regex("""^oonimkall-.*-darwin\.jar$"""),
                listOf(
                    "jniLibs/arm64/libgojni.dylib" to "gojni/darwin-aarch64/libgojni.dylib",
                    "jniLibs/amd64/libgojni.dylib" to "gojni/darwin-x86-64/libgojni.dylib",
                ),
                false,
            ),
        )

        val classpathFiles = runtimeClasspath.get().files
        plans.forEach { (jarPattern, entries, required) ->
            val jar = classpathFiles.firstOrNull { jarPattern.matches(it.name) }
            if (jar == null) {
                if (required) {
                    throw GradleException(
                        "extractMacOsNativeLibraries: could not find jar matching ${jarPattern.pattern} on desktopRuntimeClasspath",
                    )
                }
                logger.lifecycle("extractMacOsNativeLibraries: optional jar ${jarPattern.pattern} not on classpath; skipping")
                return@forEach
            }
            var extracted = 0
            ZipFile(jar).use { zip ->
                entries.forEach { (entry, relativeOut) ->
                    val ze = zip.getEntry(entry) ?: return@forEach
                    val dst = File(out, relativeOut).apply { parentFile.mkdirs() }
                    zip.getInputStream(ze).use { input ->
                        dst.outputStream().use { output -> input.copyTo(output) }
                    }
                    extracted++
                }
            }
            if (required && extracted == 0) {
                throw GradleException(
                    "extractMacOsNativeLibraries: no expected darwin entries found in ${jar.name}",
                )
            }
        }
    }
}

// Copy desktopMain resources into a per-distribution staging dir, stripping
// updater binaries (Sparkle/WinSparkle/updatebridge/libwinpthread) whenever
// the active distribution doesn't bundle an auto-updater. The same pattern
// list is used by the `verifyStoreBundle` task to audit produced packages.
val prepareDesktopResources = tasks.register<Sync>("prepareDesktopResources") {
    from(desktopResourcesDir)
    // Stage extracted native libs under macos/<lib>/<arch>/ so the Compose
    // Desktop plugin (which strips the `macos/` prefix on darwin) places them
    // at <Contents/app/resources>/<lib>/<arch>/ inside the .app.
    from(extractMacOsNativeLibraries.map { it.outputs.files.singleFile }) {
        into("macos")
    }
    into(preparedResourcesDir)
    if (!dist.bundlesSparkle || !dist.bundlesWinSparkle) {
        desktopUpdateResourcePatterns.forEach { exclude(it) }
    }
}

compose.desktop {
    application {
        mainClass = "org.ooni.probe.MainKt"

        // Compose Desktop runs ProGuard on the release distribution by default.
        // It mangles okio (`Okio__OkioKt.buffer$…` VerifyError), strips reflectively
        // referenced classes such as `org.sqlite.Function`, and renames JNI native
        // method names so the bundled `libsqlitejdbc.dylib` symbol `_open_utf8` no
        // longer matches — all observed at first launch of the Mac App Store build.
        // The proguard-rules.pro in this module is Android-only; we have no desktop
        // keep rules, and the binary-size win is marginal next to the ~250 MB
        // bundled JRE, so disable ProGuard for desktop until proper keep rules
        // exist for okio, sqlite-jdbc, JNA, and gojni.
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            if (dist.isAppStore) {
                // Pkg ships the macOS App Store build. Exe is the Windows
                // appstore-channel installer. Compose Desktop picks the format
                // that matches the current OS, so both can coexist here.
                targetFormats(TargetFormat.Pkg, TargetFormat.Exe)
            } else {
                targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
            }
            packageName = "OONI Probe"
            packageVersion = android.defaultConfig.versionName
            description =
                "OONI Probe is a free and open source software designed to measure internet censorship and other forms of network interference."
            copyright = "© ${LocalDate.now().year} OONI. All rights reserved."
            vendor = "Open Observatory of Network Interference (OONI)"
            licenseFile = rootProject.file("LICENSE")

            modules("java.sql", "jdk.unsupported")

            // Include native libraries
            includeAllModules = true

            appResourcesRootDir.fileProvider(prepareDesktopResources.map { it.destinationDir })
            val appId = "org.ooni.probe-desktop"

            macOS {
                minimumSystemVersion = "14.8.3"
                bundleID = appId
                val macDir = if (dist.requiresSandbox) "macos/appstore" else "macos/direct"
                entitlementsFile.set(project.file("$macDir/entitlements.plist"))
                runtimeEntitlementsFile.set(project.file("$macDir/runtime-entitlements.plist"))
                infoPlist {
                    extraKeysRawXml = project
                        .file("$macDir/Info.plist")
                        .readText()
                        .replace("APP_ID", appId)
                }
                packageBuildVersion = android.defaultConfig.versionCode.toString()
                jvmArgs("-Dapple.awt.enableTemplateImages=true") // tray template icon
                jvmArgs("-Dapple.awt.application.appearance=system") // adaptive title bar
                // gojni loading no longer goes through `java.library.path`:
                // `go.Seq.<clinit>` on macOS calls
                // `go.NativeUtils.loadLibraryFromJar` (not `System.loadLibrary`),
                // and our `composeApp/src/desktopMain/kotlin/go/NativeUtils.kt`
                // shadow class reads `ooni.gojni.boot.library.path` (set by
                // `configureBundledNativeLibraries`) and `System.load`s the
                // bundled, codesigned dylib directly.
                iconFile.set(rootProject.file("icons/app.icns"))
                appStore = dist.isAppStore
                if (dist.isAppStore) {
                    signing {
                        sign.set(true)
                        dist.macSigningIdentity?.let { identity.set(it) }
                    }
                    provisioningProfile.set(project.file("$macDir/embedded.provisionprofile"))
                    runtimeProvisioningProfile.set(project.file("$macDir/runtime.provisionprofile"))
                }
            }
            windows {
                iconFile.set(rootProject.file("icons/app.ico"))
                dirChooser = true
                shortcut = true
                menu = true
            }
            linux {
                iconFile.set(rootProject.file("icons/app.png"))
            }
        }
    }
}

// Set macOS DMG volume icon
tasks.withType<AbstractJPackageTask>().all {
    if (targetFormat == TargetFormat.Dmg) {
        freeArgs.addAll("--icon", rootProject.file("icons/app.icns").absolutePath)
    }
}

// Convert DMG to use LZMA compression (UDBZ) after jpackage creates it
tasks.withType<AbstractJPackageTask>().all {
    if (targetFormat == TargetFormat.Dmg) {
        doLast {
            val dmgFile = File(destinationDir.get().asFile, "${packageName.get()}-${packageVersion.get()}.dmg")
            if (dmgFile.exists()) {
                logger.lifecycle("Converting DMG to LZMA compression (UDBZ format)...")
                val tempDmg = File(destinationDir.get().asFile, "temp-${packageName.get()}-${packageVersion.get()}.dmg")

                try {
                    project.providers
                        .exec {
                            commandLine(
                                "hdiutil",
                                "convert",
                                dmgFile.absolutePath,
                                "-format",
                                "UDBZ",
                                "-o",
                                tempDmg.absolutePath,
                            )
                        }.result
                        .get()
                        .assertNormalExitValue()

                    if (!tempDmg.exists()) {
                        throw GradleException("DMG conversion succeeded but output file not found: ${tempDmg.absolutePath}")
                    }

                    if (!dmgFile.delete()) {
                        throw GradleException("Failed to delete original DMG file: ${dmgFile.absolutePath}")
                    }

                    if (!tempDmg.renameTo(dmgFile)) {
                        throw GradleException("Failed to rename converted DMG from ${tempDmg.absolutePath} to ${dmgFile.absolutePath}")
                    }

                    logger.lifecycle("Successfully converted DMG to LZMA compression (UDBZ format)")
                } catch (e: Exception) {
                    // Clean up temporary file if it exists
                    if (tempDmg.exists()) {
                        tempDmg.delete()
                    }
                    throw GradleException("Failed to convert DMG to UDBZ format: ${e.message}", e)
                }
            } else {
                logger.error("DMG file not found: ${dmgFile.absolutePath}")
                throw GradleException("Expected DMG file not found: ${dmgFile.absolutePath}")
            }
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "ooniprobe.composeapp.generated.resources"
    generateResClass = always
}

version = android.defaultConfig.versionName ?: ""

registerDesktopBuildConfigTask(
    versionName = android.defaultConfig.versionName ?: "1.0.0",
    versionCode = android.defaultConfig.versionCode ?: 0,
)
kotlin.sourceSets.getByName("desktopMain") {
    kotlin.srcDir(tasks.named("generateDesktopBuildConfig"))
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.androidx.ui.tooling.preview)
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

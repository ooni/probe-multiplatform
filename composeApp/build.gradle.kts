import com.android.build.api.variant.FilterConfiguration.FilterType.ABI
import java.time.LocalDate
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
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

    alias(libs.plugins.javafx)

    id("ooni.common")
}

val organization: String? by project

val config = Organization.fromKey(organization).config

val javaFxParts = listOf("base", "graphics", "controls", "media", "web", "swing")
val javaFxVersion = "17"

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
            version = "8.57.1"
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
                implementation(compose.desktop.currentOs)
                implementation(libs.bundles.desktop)

                // As JavaFX have platform-specific dependencies, we need to add them manually
                val fxSuffix = getJavaFxSuffix()
                javaFxParts.forEach {
                    implementation("org.openjfx:javafx-$it:$javaFxVersion:$fxSuffix")
                }
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
                optIn("kotlin.time.ExperimentalTime")
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
        versionCode = 240 // Always increment by 10. See fdroid flavor below
        versionName = "5.2.2"
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
        create("xperimental") {
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
        "fullImplementation"("org.ooni:oonimkall:3.27.0-android:@aar")
        "fdroidImplementation"("org.ooni:oonimkall:3.27.0-android:@aar")
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

compose.desktop {
    application {
        mainClass = "org.ooni.probe.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
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

            val appResource = project.layout.projectDirectory.dir("src/desktopMain/resources/")
            println(" Project directory: $appResource")
            appResourcesRootDir.set(appResource)
            val appId = "org.ooni.probe-desktop"

            macOS {
                minimumSystemVersion = "12.0.0"
                bundleID = appId
                entitlementsFile.set(project.file("OONIProbe.entitlements"))
                infoPlist {
                    extraKeysRawXml = """
                        <key>LSUIElement</key>
                        <string>true</string>
                        <key>CFBundleURLTypes</key>
                        <array>
                            <dict>
                                <key>CFBundleURLName</key>
                                <string>ooni</string>
                                <key>CFBundleURLSchemes</key>
                                <array>
                                    <string>ooni</string>
                                </array>
                            </dict>
                        </array>
                        <key>com.apple.security.app-sandbox</key>
                        <false/>
                        <key>com.apple.security.cs.allow-jit</key>
                        <true/>
                        <key>com.apple.security.cs.allow-unsigned-executable-memory</key>
                        <true/>
                        <key>com.apple.security.cs.disable-library-validation</key>
                        <true/>
                        <key>com.apple.security.cs.allow-dyld-environment-variables</key>
                        <true/>
                        <key>com.apple.security.cs.debugger</key>
                        <true/>
                        <key>com.apple.security.network.server</key>
                        <true/>
                        <key>com.apple.security.network.client</key>
                        <true/>
                        <key>com.apple.security.files.user-selected.read-write</key>
                        <true/>
                        <key>com.apple.security.files.downloads.read-write</key>
                        <true/>
                        <key>com.apple.security.temporary-exception.mach-lookup.global-name</key>
                        <array>
                            <string>$appId-spks</string>
                            <string>$appId-spki</string>
                            <string>org.sparkle-project.InstallerLauncher</string>
                            <string>org.sparkle-project.InstallerConnection</string>
                            <string>org.sparkle-project.InstallerStatus</string>
                            <string>org.sparkle-project.Downloader</string>
                            <string>com.apple.WebKit.WebContent</string>
                            <string>com.apple.WebKit.GPU</string>
                            <string>com.apple.WebKit.Networking</string>
                            <string>com.apple.installer.installer</string>
                            <string>com.apple.installer.installer.helper</string>
                        </array>
                        <key>com.apple.security.temporary-exception.mach-register.global-name</key>
                        <array>
                            <string>$appId-spks</string>
                            <string>$appId-spki</string>
                            <string>org.sparkle-project.InstallerLauncher</string>
                            <string>org.sparkle-project.InstallerConnection</string>
                            <string>org.sparkle-project.InstallerStatus</string>
                            <string>org.sparkle-project.Downloader</string>
                            <string>com.apple.WebKit.WebContent</string>
                            <string>com.apple.WebKit.GPU</string>
                            <string>com.apple.WebKit.Networking</string>
                            <string>com.apple.installer.installer</string>
                            <string>com.apple.installer.installer.helper</string>
                        </array>
                        <key>com.apple.security.temporary-exception.shared-preference.read-write</key>
                        <array>
                            <string>$appId</string>
                        </array>
                        <key>com.apple.security.cs.allow-unsigned-executable-memory</key>
                        <true/>
                        <key>com.apple.security.cs.disable-library-validation</key>
                        <true/>
                        <key>SUPublicEDKey</key>
                        <string>p1lTWmqHCTBhhCEtLT7sf/5pwS21mV3ZrvUudGnECLo=</string>
                        <key>SUEnableAutomaticChecks</key>
                        <false/>
                        <key>SUScheduledCheckInterval</key>
                        <integer>0</integer>
                        <key>SUAllowsAutomaticUpdates</key>
                        <false/>
                        <key>SUEnableInstallerLauncherService</key>
                        <true/>
                        <key>com.apple.runningboard.assertions.webkit</key>
                        <true/>
                    """.trimIndent()
                }
                jvmArgs("-Dapple.awt.enableTemplateImages=true") // tray template icon
                jvmArgs("-Dapple.awt.application.appearance=system") // adaptive title bar
                iconFile.set(rootProject.file("icons/app.icns"))
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

        // Pass properties to the JVM when running from Gradle
        jvmArgs += listOf(
            "-Dapp.version.name=${android.defaultConfig.versionName}",
            "-Dapp.version.code=${android.defaultConfig.versionCode}",
        )
    }
}

// Set macOS DMG volume icon
tasks.withType<AbstractJPackageTask>().all {
    if (targetFormat == TargetFormat.Dmg) {
        freeArgs.addAll("--icon", rootProject.file("icons/app.icns").absolutePath)
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "ooniprobe.composeapp.generated.resources"
    generateResClass = always
}

// region Work around temporary Compose bugs.
configurations.all {
    attributes {
        // https://github.com/JetBrains/compose-jb/issues/1404#issuecomment-1146894731
        attribute(Attribute.of("ui", String::class.java), "awt")
    }
}
// endregion

version = android.defaultConfig.versionName ?: ""

dependencies {
    debugImplementation(compose.uiTooling)
}

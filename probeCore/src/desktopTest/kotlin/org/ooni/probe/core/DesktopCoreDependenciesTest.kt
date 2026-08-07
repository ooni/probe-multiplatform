package org.ooni.probe.core

import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopCoreDependenciesTest {
    // Production-classpath smoke: uses the default constructor + classloader, i.e. the exact
    // path the packaged CLI distribution runs. This reads assets/descriptors/*.json from
    // probeCore's desktopMain resources (packaged into the jar), NOT test-only fixtures.
    // It guards against the T4 regression where the provider passed only because shadowing
    // desktopTest stub fixtures existed while the production artifact shipped no descriptors.
    @Test
    fun productionClasspathProvidesDecodableDescriptorAssetsForEachOrganization() =
        runTest {
            val provider = ClasspathDescriptorAssetProvider()
            val decoder = BootstrapDescriptorDecoder(
                Json {
                    encodeDefaults = true
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )

            for (assetSet in DescriptorAssetSet.entries) {
                val raw = provider.load(assetSet)
                assertTrue(
                    raw.trimStart().startsWith("["),
                    "Production descriptor asset ${assetSet.name} is not a JSON array; got: ${raw.take(40)}",
                )
                val descriptors = decoder.decode(provider, assetSet)
                assertTrue(
                    descriptors.isNotEmpty(),
                    "Production descriptor asset ${assetSet.name} decoded to an empty descriptor list",
                )
            }

            val gateway = DesktopCoreDependencies.createCliGateway() as CliCoreGatewayDependencies
            assertEquals(DescriptorAssetSet.Ooni, gateway.defaultDescriptorAssetSet)
        }

    @Test
    fun cliGatewayExposesNativeBootstrapAndClasspathShadow() {
        val gateway = DesktopCoreDependencies.createCliGateway() as CliCoreGatewayDependencies

        assertTrue(gateway.nativeRuntimeBootstrap is DesktopNativeRuntimeBootstrap)
        assertEquals(
            "go.NativeUtils",
            Class.forName("go.NativeUtils").name,
        )
    }

    @Test
    fun cliResetGatewayConstructionAndCloseNeverAccessTheKeychain() {
        // Building the reset gateway and closing it without clearing must not resolve the platform
        // SecureStorage (it is lazy). Using an unsupported OS makes the point: resolving the store
        // would throw, so a clean construct+close proves nothing native was touched.
        val gateway = DesktopCoreDependencies.createCliResetGateway(
            CliEngineConfig(
                databaseDir = "unused",
                baseFileDir = "unused",
                cacheDir = "unused",
                ooniApiBaseUrl = "https://api.ooni.io",
                baseSoftwareName = "ooniprobe",
                softwareVersion = "0.0.0",
                passportVersion = "0.0.0",
                proxy = null,
                osName = "Some Unsupported OS",
                osVersion = "0",
            ),
        )
        gateway.close()
    }

    @Test
    fun bundledNativeBootstrapConfiguresAvailableMacOsGoJniDirectory() {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return
        val root = Files.createTempDirectory("core-native-resources").toFile()
        val architecture = when (System.getProperty("os.arch").lowercase()) {
            "aarch64", "arm64" -> "darwin-aarch64"
            "x86_64", "amd64" -> "darwin-x86-64"
            else -> "darwin"
        }
        val library = root.resolve("gojni/$architecture/libgojni.dylib")
        library.parentFile.mkdirs()
        library.writeText("")

        val result = configureBundledNativeLibraries(root)

        assertEquals(listOf("gojni"), result.appliedLibraries)
        assertEquals(library.parentFile.absolutePath, System.getProperty("ooni.gojni.boot.library.path"))
        assertEquals(library.name, System.getProperty("ooni.gojni.boot.library.name"))
    }
}

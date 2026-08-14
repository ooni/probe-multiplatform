package org.ooni.probe.cli

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.TaskLogLevel
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CliRuntimeTest {
    @Test
    fun resolvesDefaultHomeAndConfigFromInjectedUserHome() = runTest {
        withTempDirectory { root ->
            val userHome = root.resolve("user-home")
            val tempDir = root.resolve("platform-temp")
            val runtime = CliRuntime.fromEnvironment(
                environment = emptyMap(),
                userHome = userHome,
                tempDir = tempDir,
            )

            assertEquals(userHome.resolve(".ooniprobe").toAbsolutePath(), runtime.paths.ooniHome)
            assertEquals(runtime.paths.ooniHome.resolve("config.json"), runtime.paths.configFile)
            assertEquals(tempDir.toAbsolutePath(), runtime.paths.tempDir)
        }
    }

    @Test
    fun externalConfigUsesDefaultHomeWhenOoniHomeIsUnset() = runTest {
        withTempDirectory { root ->
            val userHome = root.resolve("fake-user-home")
            val config = root.resolve("external/config.json")
            val runtime = CliRuntime.fromEnvironment(
                environment = emptyMap(),
                userHome = userHome,
                tempDir = root.resolve("temp"),
            ).withInvocation(CliInvocationOptions(configFile = config))

            assertEquals(userHome.resolve(".ooniprobe").toAbsolutePath(), runtime.paths.ooniHome)
            assertEquals(config.toAbsolutePath(), runtime.paths.configFile)
        }
    }

    @Test
    fun ooniHomeEnvironmentDoesNotFollowExternalConfigParent() = runTest {
        withTempDirectory { root ->
            val configuredHome = root.resolve("profile")
            val externalConfig = root.resolve("external/config.json")
            val runtime = CliRuntime.fromEnvironment(
                environment = mapOf("OONI_HOME" to configuredHome.toString()),
                userHome = root.resolve("ignored-user-home"),
                tempDir = root.resolve("temp"),
            ).withInvocation(CliInvocationOptions(configFile = externalConfig))

            assertEquals(configuredHome.toAbsolutePath(), runtime.paths.ooniHome)
            assertEquals(externalConfig.toAbsolutePath(), runtime.paths.configFile)
            assertEquals(configuredHome.resolve("data/probe.db").toAbsolutePath(), runtime.paths.databaseFile)
            assertFalse(runtime.paths.ooniHome.startsWith(externalConfig.parent))
        }
    }

    @Test
    fun pathLayoutOwnsTaskSettingsAndResetPaths() = runTest {
        withTempDirectory { root ->
            val home = root.resolve("home")
            val externalConfig = root.resolve("external/config.json")
            val externalRuntime = runtime(home, externalConfig, root.resolve("temp"))
            val internalRuntime = runtime(home, home.resolve("config.json"), home.resolve("tmp"))

            assertEquals(home.resolve("data/state").toAbsolutePath(), externalRuntime.paths.taskSettingsPaths().stateDir)
            assertEquals(root.resolve("temp").toAbsolutePath(), externalRuntime.paths.taskSettingsPaths().tempDir)
            assertEquals(home.resolve("tunnel").toAbsolutePath(), externalRuntime.paths.taskSettingsPaths().tunnelDir)
            assertEquals(home.resolve("assets").toAbsolutePath(), externalRuntime.paths.taskSettingsPaths().assetsDir)
            assertEquals(home.resolve("assets/geoip.mmdb").toAbsolutePath(), externalRuntime.paths.taskSettingsPaths().geoIpDb)
            assertFalse(externalRuntime.paths.resetDeletionSet().contains(externalConfig.toAbsolutePath()))
            assertFalse(externalRuntime.paths.resetDeletionSet().contains(root.resolve("temp").toAbsolutePath()))
            assertTrue(internalRuntime.paths.resetDeletionSet().contains(home.resolve("config.json").toAbsolutePath()))
            assertTrue(internalRuntime.paths.resetDeletionSet().contains(home.resolve("tmp").toAbsolutePath()))
        }
    }

    @Test
    fun runtimeIdentityAndProxyMapToTaskSettingsInputs() = runTest {
        withTempDirectory { root ->
            val runtime = runtime(
                home = root.resolve("home"),
                config = root.resolve("config.json"),
                temp = root.resolve("temp"),
            ).withInvocation(
                CliInvocationOptions(
                    softwareName = "custom-probe",
                    softwareVersion = "1.2.3",
                    proxy = "http://127.0.0.1:8080",
                    verbose = true,
                ),
            ).copy(networkTypeFinder = { NetworkType.Unknown("unknown") })

            val mapping = runtime.taskSettingsMapping()
            assertEquals(TaskLogLevel.Debug, mapping.logLevel)
            assertEquals("custom-probe", mapping.options.softwareName)
            assertEquals("1.2.3", mapping.options.softwareVersion)
            assertTrue(mapping.options.noCollector)
            assertEquals(-1, mapping.options.maxRuntime)
            assertEquals("custom-probe", mapping.annotations.flavor)
            assertEquals("ooni-run", mapping.annotations.origin.value)
            assertEquals("unknown", mapping.annotations.networkType.value)
            assertEquals(System.getProperty("os.version"), mapping.annotations.osVersion)
            assertEquals("http://127.0.0.1:8080", mapping.proxy)
            assertEquals(runtime.paths.taskSettingsPaths(), mapping.paths)
        }
    }

    @Test
    fun invocationFlagsRemainInMemoryAndLeaveConfigAndPreferencesUntouched() = runTest {
        withTempDirectory { root ->
            val config = root.resolve("external/config.json")
            Files.createDirectories(config.parent)
            Files.writeString(config, "{\"first_run\":true}")
            val runtime = runtime(root.resolve("home"), config, root.resolve("temp")).withInvocation(
                CliInvocationOptions(
                    softwareName = "custom",
                    softwareVersion = "1.2.3",
                    batch = true,
                    verbose = true,
                    logHandler = null,
                    jsonOutput = true,
                ),
            )
            val store = preferenceStore(runtime.paths.preferenceDataStoreFile)
            try {
                assertEquals("{\"first_run\":true}", Files.readString(config))
                assertFalse(store.repository.contains(SettingsKey.FIRST_RUN))
                assertEquals(CliLogHandler.Batch, runtime.logHandler)
                assertTrue(CliConfigContract.supportedDefaultKeys.isEmpty())
            } finally {
                store.close()
            }
        }
    }

    @Test
    fun corePreferencesPersistAcrossRuntimeInstancesWithoutConfigImport() = runTest {
        withTempDirectory { root ->
            val home = root.resolve("home")
            val config = root.resolve("external/config.json")
            Files.createDirectories(config.parent)
            Files.writeString(config, "{\"first_run\":true}")
            val firstRuntime = runtime(home, config, root.resolve("temp"))
            val firstStore = preferenceStore(firstRuntime.paths.preferenceDataStoreFile)
            try {
                assertNull(firstStore.repository.getValueByKey(SettingsKey.FIRST_RUN).first())
                firstStore.repository.setValueByKey(SettingsKey.FIRST_RUN, false)
            } finally {
                firstStore.close()
            }

            val secondRuntime = runtime(home, config, root.resolve("temp"))
            val secondStore = preferenceStore(secondRuntime.paths.preferenceDataStoreFile)
            try {
                assertEquals(false, secondStore.repository.getValueByKey(SettingsKey.FIRST_RUN).first())
                assertEquals("{\"first_run\":true}", Files.readString(config))
            } finally {
                secondStore.close()
            }
        }
    }

    private fun runtime(
        home: Path,
        config: Path,
        temp: Path,
    ): CliRuntime =
        CliRuntime.fromEnvironment(
            environment = mapOf("OONI_HOME" to home.toString()),
            userHome = home.resolveSibling("unused-user-home"),
            tempDir = temp,
        ).withInvocation(CliInvocationOptions(configFile = config))

    private fun preferenceStore(path: Path): PreferenceStore {
        Files.createDirectories(path.parent)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { path.toFile() }
        return PreferenceStore(PreferenceRepository(dataStore), scope)
    }

    private suspend fun withTempDirectory(block: suspend (Path) -> Unit) {
        val directory = Files.createTempDirectory("cli-runtime-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private class PreferenceStore(
        val repository: PreferenceRepository,
        private val scope: CoroutineScope,
    ) {
        fun close() {
            scope.cancel()
            scope.coroutineContext[Job]?.cancel()
        }
    }
}

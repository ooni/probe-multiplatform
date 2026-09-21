package org.ooni.probe.cli

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.ooni.probe.core.CliStorageConfig
import org.ooni.probe.core.JsonFilePreferencesDataStore
import org.ooni.probe.core.buildDesktopCliStorageGateway
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * T8 onboarding audit: drives the REAL production storage gateway end-to-end through the CLI
 * against a temp OONI home, then reads the persisted state back through core APIs (a freshly
 * constructed core storage gateway and the core [PreferenceRepository]).
 *
 * This proves onboarding writes the exact state `GetFirstRun` reads as not-first-run
 * (`SettingsKey.FIRST_RUN == false`) and that the batch-without-consent path writes NOTHING. It
 * complements the fake-gateway onboarding tests in `CliStorageCommandsTest` (which assert command
 * wiring) by exercising real durable persistence. The `run` onboarding preflight (batch run without
 * consent fails before the run gateway) is owned by
 * `RunCommandFailureTest.batchRunWithIncompleteOnboardingFailsBeforeRunGateway`.
 *
 * All state lives under a temp directory; the real `~/.ooniprobe` is never touched.
 */
class OnboardingPersistenceTest {
    @Test
    fun onboardYesPersistsCompletionReadableByAFreshCoreGateway() = withTempHome { home ->
        val runtime = runtimeFor(home)
        val result = runCli(runtime, "onboard", "--yes")

        assertEquals(0, result.code)
        assertTrue(result.stdout.any { it.contains("Onboarding complete") }, result.stdout.toString())
        // Read back through a FRESH core storage gateway pointed at the same temp home.
        assertTrue(runBlocking { readOnboardingComplete(runtime.paths) })
    }

    @Test
    fun onboardYesWritesFirstRunFalseThroughTheCorePreferenceApi() = withTempHome { home ->
        val runtime = runtimeFor(home)
        assertEquals(0, runCli(runtime, "onboard", "--yes").code)

        // The exact state GetFirstRun reads: SettingsKey.FIRST_RUN persisted as false (not-first-run).
        assertEquals(false, runBlocking { readFirstRun(runtime.paths) })
    }

    @Test
    fun batchOnboardWithoutYesFailsAndWritesNoOnboardingPreference() = withTempHome { home ->
        val runtime = runtimeFor(home)
        val result = runCli(runtime, "--batch", "onboard")

        // Deterministic failure: --yes is required in batch mode.
        assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
        assertTrue(result.stderr.any { it.contains("--yes") }, result.stderr.toString())
        // No onboarding preference was written: the FIRST_RUN key is absent (still first-run).
        assertFalse(runBlocking { firstRunKeyExists(runtime.paths) })
        assertFalse(runBlocking { readOnboardingComplete(runtime.paths) })
    }

    @Test
    fun onboardIsIdempotentWhenAlreadyComplete() = withTempHome { home ->
        val runtime = runtimeFor(home)
        assertEquals(0, runCli(runtime, "onboard", "--yes").code)

        val second = runCli(runtime, "onboard")

        assertEquals(0, second.code)
        assertTrue(second.stdout.any { it.contains("already complete") }, second.stdout.toString())
        // Still complete; the idempotent re-run neither cleared nor re-prompted for consent.
        assertEquals(false, runBlocking { readFirstRun(runtime.paths) })
    }

    // ---- harness -------------------------------------------------------------------------------

    private fun runtimeFor(home: Path): CliRuntime =
        CliRuntime(paths = CliPathLayout.create(ooniHome = home, tempDir = home.resolve("tmp")))

    /** Runs the CLI with the DEFAULT production factories (real SQLDelight + DataStore gateway). */
    private fun runCli(runtime: CliRuntime, vararg args: String): CliResult {
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()
        val code = OoniprobeCli(runtime = runtime, input = { null })
            .run(args.toList().toTypedArray(), stdout::add, stderr::add)
        return CliResult(code, stdout, stderr)
    }

    private suspend fun readOnboardingComplete(paths: CliPathLayout): Boolean {
        val gateway = buildDesktopCliStorageGateway(
            CliStorageConfig(
                databaseDir = paths.dataDir.toString(),
                preferencesFile = paths.preferenceDataStoreFile.toString(),
            ),
        )
        return try {
            gateway.isOnboardingComplete()
        } finally {
            gateway.close()
        }
    }

    // getValueByKey returns Flow<Any?>; FIRST_RUN is a boolean preference (false == not-first-run).
    private suspend fun readFirstRun(paths: CliPathLayout): Any? =
        preferenceRepository(paths.preferenceDataStoreFile).getValueByKey(SettingsKey.FIRST_RUN).first()

    private suspend fun firstRunKeyExists(paths: CliPathLayout): Boolean =
        preferenceRepository(paths.preferenceDataStoreFile).contains(SettingsKey.FIRST_RUN)

    private fun preferenceRepository(path: Path): PreferenceRepository {
        Files.createDirectories(path.parent)
        return PreferenceRepository(JsonFilePreferencesDataStore(path.toFile()))
    }

    private fun withTempHome(block: (Path) -> Unit) {
        val home = Files.createTempDirectory("cli-onboard-test")
        try {
            block(home)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    private data class CliResult(
        val code: Int,
        val stdout: List<String>,
        val stderr: List<String>,
    )
}

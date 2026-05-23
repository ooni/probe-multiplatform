package org.ooni.probe.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.SettingsKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveAndConfigureRunAtStartupTest {
    private val dispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())

    /** In-memory run-at-startup preference store. */
    private val preferences = MutableStateFlow<Map<SettingsKey, Any?>>(emptyMap())

    /** Auto-run settings the subject observes (stands in for GetAutoRunSettings). */
    private val autoRun = MutableStateFlow<AutoRunParameters>(AutoRunParameters.Disabled)

    /** Records every preference write, and every OS "run at startup" effect call. */
    private val writtenKeys = mutableListOf<SettingsKey>()
    private val configureCalls = mutableListOf<Boolean>()

    private fun buildSubject() =
        ObserveAndConfigureRunAtStartup(
            backgroundContext = dispatcher,
            getRunAtStartupSettings = {
                preferences.map { it[SettingsKey.RUN_AT_STARTUP] == true }.distinctUntilChanged()
            },
            getAutoRunSettings = { autoRun },
            setPreference = { key, value ->
                writtenKeys.add(key)
                preferences.update { it + (key to value) }
            },
            configureRunAtStartup = { enabled -> configureCalls.add(enabled) },
        )

    private val enabled get() = AutoRunParameters.Enabled(wifiOnly = false, onlyWhileCharging = false)

    @Test
    fun enablingAutoRunEnablesRunAtStartup() =
        runTest(dispatcher) {
            buildSubject().invoke()
            advanceUntilIdle()

            autoRun.value = enabled
            advanceUntilIdle()

            assertEquals(true, preferences.value[SettingsKey.RUN_AT_STARTUP])
            assertEquals(true, configureCalls.last())
        }

    @Test
    fun disablingAutoRunLeavesRunAtStartupUnchanged() =
        runTest(dispatcher) {
            preferences.value = mapOf(SettingsKey.RUN_AT_STARTUP to true)
            autoRun.value = enabled

            buildSubject().invoke()
            advanceUntilIdle()

            autoRun.value = AutoRunParameters.Disabled
            advanceUntilIdle()

            assertEquals(true, preferences.value[SettingsKey.RUN_AT_STARTUP])
        }

    @Test
    fun runAtStartupPreferenceDrivesTheLoginItem() =
        runTest(dispatcher) {
            preferences.value = mapOf(SettingsKey.RUN_AT_STARTUP to false)

            buildSubject().invoke()
            advanceUntilIdle()

            preferences.update { it + (SettingsKey.RUN_AT_STARTUP to true) }
            advanceUntilIdle()
            preferences.update { it + (SettingsKey.RUN_AT_STARTUP to false) }
            advanceUntilIdle()

            assertEquals(listOf(false, true, false), configureCalls)
        }

    @Test
    fun onlyTheRunAtStartupPreferenceIsEverWritten() =
        runTest(dispatcher) {
            buildSubject().invoke()
            advanceUntilIdle()

            autoRun.value = enabled
            advanceUntilIdle()

            assertTrue(writtenKeys.all { it == SettingsKey.RUN_AT_STARTUP })
        }
}

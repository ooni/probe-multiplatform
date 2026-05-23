package org.ooni.probe.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetRunAtStartupSettingsTest {
    private fun getRunAtStartupSettings(vararg runAtStartupValues: Any?) =
        GetRunAtStartupSettings(getPreference = { flowOf(*runAtStartupValues) })

    @Test
    fun onWhenPreferenceIsTrue() =
        runTest {
            assertEquals(true, getRunAtStartupSettings(true)().first())
        }

    @Test
    fun offWhenPreferenceIsFalse() =
        runTest {
            assertEquals(false, getRunAtStartupSettings(false)().first())
        }

    @Test
    fun offWhenPreferenceIsNotSet() =
        runTest {
            assertEquals(false, getRunAtStartupSettings(null)().first())
        }

    @Test
    fun emitsDistinctValuesOnly() =
        runTest {
            assertEquals(listOf(true), getRunAtStartupSettings(true, true)().toList())
        }
}

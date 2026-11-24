package org.ooni.probe.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.SettingsKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class BackgroundWorkLimitTest {
    private val key = SettingsKey.BACKGROUND_WORK_LIMIT_REACHED_AT
    private val stored = MutableStateFlow<Any?>(null)
    private var now = 1_000_000_000L

    private fun limit() =
        BackgroundWorkLimit(
            getPreference = {
                assertEquals(key, it)
                stored
            },
            setPreference = { k, v ->
                assertEquals(key, k)
                stored.value = v
            },
            removePreference = {
                assertEquals(key, it)
                stored.value = null
            },
            now = { now },
        )

    @Test
    fun notExhaustedByDefault() =
        runTest {
            assertFalse(limit().isExhausted())
        }

    @Test
    fun exhaustedAfterMarkReached() =
        runTest {
            val limit = limit()
            limit.markReached()
            assertEquals(now, stored.value)
            assertTrue(limit.isExhausted())
        }

    @Test
    fun exhaustedFromStoredValue() =
        runTest {
            stored.value = now - 23.hours.inWholeMilliseconds
            assertTrue(limit().isExhausted())
        }

    @Test
    fun notExhaustedAfter24Hours() =
        runTest {
            val limit = limit()
            limit.markReached()
            now += 24.hours.inWholeMilliseconds
            assertFalse(limit.isExhausted())
        }

    @Test
    fun notExhaustedIfClockWentBack() =
        runTest {
            val limit = limit()
            limit.markReached()
            now -= 1
            assertFalse(limit.isExhausted())
        }

    @Test
    fun resetClearsLimit() =
        runTest {
            val limit = limit()
            limit.markReached()
            limit.reset()
            assertNull(stored.value)
            assertFalse(limit.isExhausted())
        }
}

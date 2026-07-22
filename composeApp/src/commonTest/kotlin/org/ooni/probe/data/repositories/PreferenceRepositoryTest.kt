package org.ooni.probe.data.repositories

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.SettingsKey
import org.ooni.testing.createPreferenceDataStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreferenceRepositoryTest {
    private lateinit var preferenceRepository: PreferenceRepository

    @BeforeTest
    fun before() {
        preferenceRepository = PreferenceRepository(createPreferenceDataStore())
    }

    @AfterTest
    fun after() =
        runTest {
            preferenceRepository.clear()
        }

    @Test
    fun testAllSettings() =
        runTest {
            val value = "value"
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, value)
            val setting: Map<SettingsKey, Any?> = preferenceRepository.allSettings(listOf(SettingsKey.LANGUAGE_SETTING)).first()
            assertEquals(value, setting.values.first())
        }

    @Test
    fun testGetPreferenceKey() {
        assertEquals(
            SettingsKey.LANGUAGE_SETTING.value,
            preferenceRepository.getPreferenceKey(SettingsKey.LANGUAGE_SETTING.value),
        )
        assertEquals(
            "prefix_${SettingsKey.LANGUAGE_SETTING.value}",
            preferenceRepository.getPreferenceKey(SettingsKey.LANGUAGE_SETTING.value, "prefix"),
        )
        assertEquals(
            "autorun_${SettingsKey.LANGUAGE_SETTING.value}",
            preferenceRepository.getPreferenceKey(
                SettingsKey.LANGUAGE_SETTING.value,
                autoRun = true,
            ),
        )
        assertEquals(
            "prefix_autorun_${SettingsKey.LANGUAGE_SETTING.value}",
            preferenceRepository.getPreferenceKey(
                SettingsKey.LANGUAGE_SETTING.value,
                "prefix",
                true,
            ),
        )
    }

    @Test
    fun testGetValueByKey() =
        runTest {
            val value = "value"
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, value)
            assertEquals(value, preferenceRepository.getValueByKey(key = SettingsKey.LANGUAGE_SETTING).first())
        }

    /**
     * Timestamp keys must be mapped to LongKey explicitly. Anything not listed falls through to
     * the `else -> BooleanKey` branch, and writing a Long to it throws ClassCastException at
     * runtime - a crash no test using a fake preference map would ever catch.
     */
    @Test
    fun timestampKeysRoundTripAsLongs() =
        runTest {
            val timestampKeys = listOf(
                SettingsKey.LAST_ARTICLES_REFRESH,
                SettingsKey.LAST_ARTICLES_REFRESH_ATTEMPT,
                SettingsKey.MMDB_LAST_CHECK,
            )

            timestampKeys.forEach { key ->
                preferenceRepository.setValueByKey(key, 1_700_000_000L)
                assertEquals(
                    1_700_000_000L,
                    preferenceRepository.getValueByKey(key).first(),
                    "$key must store a Long",
                )
            }
        }

    @Test
    fun testSetValueByKey() =
        runTest {
            val value = "value"
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, value)
            assertEquals(value, preferenceRepository.getValueByKey(SettingsKey.LANGUAGE_SETTING).first())
        }

    @Test
    fun testClear() =
        runTest {
            val value = "value"
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, value)
            preferenceRepository.clear()
            assertNull(preferenceRepository.getValueByKey(SettingsKey.LANGUAGE_SETTING).first())
        }

    @Test
    fun testRemove() =
        runTest {
            val value = "value"
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, value)
            preferenceRepository.remove(SettingsKey.LANGUAGE_SETTING)
            assertNull(preferenceRepository.getValueByKey(SettingsKey.LANGUAGE_SETTING).first())
        }

    @Test
    fun testContains() =
        runTest {
            val value = "value"
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, value)
            assertEquals(true, preferenceRepository.contains(SettingsKey.LANGUAGE_SETTING))
        }
}

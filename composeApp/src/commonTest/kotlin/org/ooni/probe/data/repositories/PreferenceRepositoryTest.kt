package org.ooni.probe.data.repositories

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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
    fun after() = runBlocking {
        runTest {
            preferenceRepository.clear()
        }
    }

    @Test
    fun testAllSettings() = runBlocking {
        runTest {
            val key = stringPreferencesKey(SettingsKey.LANGUAGE_SETTING.value)
            val value = "value"
            preferenceRepository.setValueByKey(key, value)
            val setting: Map<String, Any?> = preferenceRepository.allSettings(listOf(key)).first()
            assertEquals(value, setting.values.first())
        }
    }

    @Test
    fun testGetPreferenceKey() {
        assertEquals(
            SettingsKey.LANGUAGE_SETTING.value,
            preferenceRepository.getPreferenceKey(SettingsKey.LANGUAGE_SETTING.value)
        )
        assertEquals(
            "prefix_${SettingsKey.LANGUAGE_SETTING.value}",
            preferenceRepository.getPreferenceKey(SettingsKey.LANGUAGE_SETTING.value, "prefix"),
        )
        assertEquals(
            "${SettingsKey.LANGUAGE_SETTING.value}_autorun",
            preferenceRepository.getPreferenceKey(
                SettingsKey.LANGUAGE_SETTING.value, autoRun = true
            ),
        )
        assertEquals(
            "prefix_${SettingsKey.LANGUAGE_SETTING.value}_autorun",
            preferenceRepository.getPreferenceKey(
                SettingsKey.LANGUAGE_SETTING.value, "prefix", true
            ),
        )
    }

    @Test
    fun testGetValueByKey() = runTest {
        val key = stringPreferencesKey(SettingsKey.LANGUAGE_SETTING.value)
        val value = "value"
        preferenceRepository.setValueByKey(key, value)
        assertEquals(value, preferenceRepository.getValueByKey(key = key).first())
    }


    @Test
    fun testSetValueByKey() = runTest {
        val key = stringPreferencesKey(SettingsKey.LANGUAGE_SETTING.value)
        val value = "value"
        preferenceRepository.setValueByKey(key, value)
        assertEquals(value, preferenceRepository.getValueByKey(key).first())
    }

    @Test
    fun testClear() = runTest {
        val key = stringPreferencesKey(SettingsKey.LANGUAGE_SETTING.value)
        val value = "value"
        preferenceRepository.setValueByKey(key, value)
        preferenceRepository.clear()
        assertNull(preferenceRepository.getValueByKey(key).first())
    }

    @Test
    fun testRemove() = runTest {
        val key = stringPreferencesKey(SettingsKey.LANGUAGE_SETTING.value)
        val value = "value"
        preferenceRepository.setValueByKey(key, value)
        preferenceRepository.remove(key)
        assertNull(preferenceRepository.getValueByKey(key).first())
    }

    @Test
    fun testContains() = runBlocking {
        runTest {
            val key = stringPreferencesKey(SettingsKey.LANGUAGE_SETTING.value)
            val value = "value"
            preferenceRepository.setValueByKey(key, value)
            assertEquals(true, preferenceRepository.contains(key))
        }
    }
}

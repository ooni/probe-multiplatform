package org.ooni.probe.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import org.ooni.probe.shared.today
import org.ooni.testing.createPreferenceDataStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppReviewRepositoryTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: AppReviewRepository

    @BeforeTest
    fun before() {
        dataStore = createPreferenceDataStore()
        repository = AppReviewRepository(dataStore)
    }

    @AfterTest
    fun after() =
        runTest {
            dataStore.edit { it.clear() }
        }

    @Test
    fun launchTimes() =
        runTest {
            assertEquals(0, repository.getLaunchTimes())
            repository.incrementLaunchTimes()
            repository.incrementLaunchTimes()
            repository.incrementLaunchTimes()
            assertEquals(3, repository.getLaunchTimes())
        }

    @Test
    fun firstOpenAt() =
        runTest {
            assertNull(repository.getFirstOpenAt())
            val today = LocalDate.today().atTime(0, 0)
            repository.setFirstOpenAt(today)
            assertEquals(today, repository.getFirstOpenAt())
        }

    @Test
    fun shownAt() =
        runTest {
            assertNull(repository.getShownAt())
            val today = LocalDate.today().atTime(0, 0)
            repository.setShownAt(today)
            assertEquals(today, repository.getShownAt())
        }
}

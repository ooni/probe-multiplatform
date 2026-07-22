package org.ooni.probe.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.data.models.GetBytesException
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.di.Dependencies
import org.ooni.testing.createPreferenceDataStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FetchGeoIpDbUpdatesTest {
    private lateinit var preferences: PreferenceRepository

    @BeforeTest
    fun before() {
        preferences = PreferenceRepository(createPreferenceDataStore())
    }

    @AfterTest
    fun after() =
        runTest {
            preferences.clear()
        }

    @Test
    fun offlineLookupDoesNotDownloadOrStampFreshness() =
        runTest {
            var downloadsAttempted = 0
            val subject = subject(
                passportGet = { Failure(PassportException.Offline("no active network")) },
                downloadFile = { _, _ ->
                    downloadsAttempted++
                    Failure(GetBytesException(Exception("must not be reached")))
                },
            )

            val result = subject()

            assertEquals(0, downloadsAttempted)
            assertTrue(result is Failure, "an offline lookup must not report success")
            assertNoFreshnessStamped()
        }

    @Test
    fun failedReleaseLookupDoesNotStampFreshness() =
        runTest {
            val subject = subject(
                passportGet = { Failure(PassportException.HttpClientError("HTTP 503")) },
                downloadFile = { _, _ -> Failure(GetBytesException(Exception("unused"))) },
            )

            val result = subject()

            assertTrue(result is Failure)
            assertNoFreshnessStamped()
        }

    @Test
    fun failedBinaryDownloadDoesNotStampVersionOrFreshness() =
        runTest {
            var downloadsAttempted = 0
            val subject = subject(
                // NOTE: a download is only attempted when the remote tag is *older* than the
                // current one, because `isGeoIpDbLatest` returns `latestTag >= currentTag` and the
                // caller treats that as "already up to date". That comparison looks inverted, but
                // it is pre-existing behaviour and out of scope here; this test uses the route
                // that actually reaches the download so the failure handling is covered.
                passportGet = { Success(response("""{"tag_name":"20250101"}""")) },
                downloadFile = { _, _ ->
                    downloadsAttempted++
                    Failure(GetBytesException(Exception("connection reset")))
                },
            )

            val result = subject()

            assertEquals(1, downloadsAttempted)
            assertTrue(result is Failure, "a failed download must not report success")
            assertNoFreshnessStamped()
        }

    private suspend fun assertNoFreshnessStamped() {
        assertNull(
            preferences.getValueByKey(SettingsKey.MMDB_LAST_CHECK).first(),
            "MMDB_LAST_CHECK must not be stamped after a failure, or the retry is suppressed 24h",
        )
        assertNull(
            preferences.getValueByKey(SettingsKey.MMDB_VERSION).first(),
            "MMDB_VERSION must not advance unless the database was actually written",
        )
    }

    private fun subject(
        passportGet: suspend (String) -> Result<PassportHttpResponse, PassportException>,
        downloadFile: suspend (String, String) -> Result<okio.Path, GetBytesException>,
    ) = FetchGeoIpDbUpdates(
        downloadFile = downloadFile,
        cacheDir = "/tmp/ooni-test-cache",
        passportGet = passportGet,
        preferencesRepository = preferences,
        json = Dependencies.buildJson(),
        fileSystem = FileSystem.SYSTEM,
        backgroundContext = Dispatchers.Unconfined,
    )

    private fun response(body: String) =
        PassportHttpResponse(
            statusCode = 200,
            version = "HTTP/1.1",
            headersListText = emptyList(),
            bodyText = body,
        )
}

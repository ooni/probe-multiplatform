package org.ooni.probe.domain

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.isOfflineFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DownloadFileTest {
    @Test
    fun offlineNeverBuildsAnHttpClient() =
        runTest {
            var clientsCreated = 0
            val subject = subject(
                isOnline = false,
                onClientRequested = { clientsCreated++ },
            )

            val result = subject.fetchBytes(URL)

            assertEquals(0, clientsCreated, "no Ktor client may be created while offline")
            assertIs<PassportException.Offline>(result.getError()?.cause)
        }

    @Test
    fun offlineDownloadFailsAsAnOfflineFailure() =
        runTest {
            val subject = subject(isOnline = false)

            val result = subject(URL, "/tmp/does-not-matter.mmdb")

            // Classified so FetchGeoIpDbUpdates can log it quietly and skip stamping preferences.
            assertTrue(result.getError().isOfflineFailure())
        }

    /**
     * The filesystem is only reachable through `map`, which a Failure short-circuits, so an
     * offline download cannot create directories or truncate an existing database.
     */
    @Test
    fun offlineDownloadDoesNotTouchTheFilesystem() =
        runTest {
            val subject = subject(isOnline = false)

            val result = subject(URL, "/proc/definitely/not/writable/db.mmdb")

            assertTrue(result.getError().isOfflineFailure())
        }

    private fun subject(
        isOnline: Boolean,
        onClientRequested: () -> Unit = {},
    ) = DownloadFile(
        // Never reached in these tests: every case fails before the filesystem is used.
        fileSystem = FileSystem.SYSTEM,
        isOnline = { isOnline },
        httpClientFactory = {
            onClientRequested()
            error("HTTP client must not be created while offline")
        },
    )

    companion object {
        private const val URL = "https://example.org/20250801.mmdb"
    }
}

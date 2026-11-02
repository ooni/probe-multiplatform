package org.ooni.probe.data.repositories

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.ooni.engine.DeleteResult
import org.ooni.engine.WriteResult
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.data.models.SessionState
import org.ooni.probe.di.Dependencies
import org.ooni.probe.domain.auth.AuthException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class SessionRepositoryTest {
    private val json = Dependencies.buildJson()
    private var inMemoryStorage: String? = null
    private var shouldFailRead = false

    private val repository = SessionRepository(
        readSecureStorage = {
            if (shouldFailRead) throw IllegalStateException("Storage error")
            inMemoryStorage
        },
        writeSecureStorage = { key, value ->
            inMemoryStorage = value
            WriteResult.Updated(key)
        },
        deleteSecureStorage = { key ->
            inMemoryStorage = null
            DeleteResult.Deleted(key)
        },
        json = json,
    )

    private val session = AuthSession(
        sessionToken = "token-123",
        emailAddress = "user@example.org",
        role = "user",
        loginTime = Instant.parse("2025-01-01T00:00:00Z"),
    )

    @Test
    fun sessionStateEmitsCorrectStateTransitions() =
        runTest {
            val emitted = mutableListOf<SessionState>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.sessionState.collect { emitted.add(it) }
            }

            assertEquals(listOf<SessionState>(SessionState.LoggedOut), emitted)

            repository.store(session)
            assertEquals(
                listOf<SessionState>(SessionState.LoggedOut, SessionState.LoggedIn(session)),
                emitted,
            )

            repository.clear()
            assertEquals(
                listOf<SessionState>(
                    SessionState.LoggedOut,
                    SessionState.LoggedIn(session),
                    SessionState.LoggedOut,
                ),
                emitted,
            )

            job.cancel()
        }

    @Test
    fun sessionStateEmitsLoggedOutWhenReadFails() =
        runTest {
            shouldFailRead = true
            val state = repository.sessionState.first()
            assertEquals(SessionState.LoggedOut, state)
        }

    @Test
    fun readSecureStorageIsOnlyCalledOnce() =
        runTest {
            var readCount = 0
            val repo = SessionRepository(
                readSecureStorage = {
                    readCount++
                    inMemoryStorage
                },
                writeSecureStorage = { key, value ->
                    inMemoryStorage = value
                    WriteResult.Updated(key)
                },
                deleteSecureStorage = { key ->
                    inMemoryStorage = null
                    DeleteResult.Deleted(key)
                },
                json = json,
            )

            assertEquals(SessionState.LoggedOut, repo.sessionState.first())
            assertEquals(SessionState.LoggedOut, repo.sessionState.first())
            repo.store(session)
            assertEquals(SessionState.LoggedIn(session), repo.sessionState.first())
            repo.clear()
            assertEquals(SessionState.LoggedOut, repo.sessionState.first())

            assertEquals(1, readCount)
        }

    @Test
    fun failedReadIsNotCached() =
        runTest {
            var readCount = 0
            var fail = true
            val repo = SessionRepository(
                readSecureStorage = {
                    readCount++
                    if (fail) throw IllegalStateException("Locked")
                    inMemoryStorage
                },
                writeSecureStorage = { key, value ->
                    inMemoryStorage = value
                    WriteResult.Updated(key)
                },
                deleteSecureStorage = { key ->
                    inMemoryStorage = null
                    DeleteResult.Deleted(key)
                },
                json = json,
            )

            assertEquals(SessionState.LoggedOut, repo.sessionState.first())
            assertEquals(1, readCount)

            // Once storage unlocks / succeeds:
            fail = false
            inMemoryStorage = json.encodeToString(AuthSession.serializer(), session)

            assertEquals(SessionState.LoggedIn(session), repo.sessionState.first())
            assertEquals(2, readCount)
        }

    @Test
    fun loadFromStorageDoesNotOverwriteConcurrentStore() =
        runTest {
            val readStarted = CompletableDeferred<Unit>()
            val storeDone = CompletableDeferred<Unit>()
            var readCount = 0

            val repo = SessionRepository(
                readSecureStorage = {
                    readCount++
                    readStarted.complete(Unit)
                    storeDone.await()
                    null
                },
                writeSecureStorage = { key, value ->
                    inMemoryStorage = value
                    WriteResult.Updated(key)
                },
                deleteSecureStorage = { key ->
                    inMemoryStorage = null
                    DeleteResult.Deleted(key)
                },
                json = json,
            )

            val job = launch {
                repo.sessionState.first()
            }

            readStarted.await()
            repo.store(session)
            storeDone.complete(Unit)
            job.join()

            assertEquals(SessionState.LoggedIn(session), repo.sessionState.first())
        }

    @Test
    fun clearDoesNotSetLoggedOutOnDeleteError() =
        runTest {
            val repo = SessionRepository(
                readSecureStorage = { inMemoryStorage },
                writeSecureStorage = { key, value ->
                    inMemoryStorage = value
                    WriteResult.Updated(key)
                },
                deleteSecureStorage = { key ->
                    DeleteResult.Error(key, "Delete failed")
                },
                json = json,
            )

            repo.store(session)
            assertEquals(SessionState.LoggedIn(session), repo.sessionState.first())

            repo.clear()
            // Since delete returned Error, session state remains LoggedIn in memory
            assertEquals(SessionState.LoggedIn(session), repo.sessionState.first())
        }

    @Test
    fun longLivedCollectorRetriesFailedRead() =
        runTest {
            var fail = true
            val repo = SessionRepository(
                readSecureStorage = {
                    if (fail) throw IllegalStateException("Locked")
                    inMemoryStorage
                },
                writeSecureStorage = { key, value ->
                    inMemoryStorage = value
                    WriteResult.Updated(key)
                },
                deleteSecureStorage = { key ->
                    inMemoryStorage = null
                    DeleteResult.Deleted(key)
                },
                json = json,
                readRetryDelay = 5.seconds,
            )

            val emitted = mutableListOf<SessionState>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                repo.sessionState.collect { emitted.add(it) }
            }
            assertEquals(listOf<SessionState>(SessionState.LoggedOut), emitted)

            // Storage unlocks
            fail = false
            inMemoryStorage = json.encodeToString(AuthSession.serializer(), session)
            advanceTimeBy(6.seconds)

            assertEquals(listOf(SessionState.LoggedOut, SessionState.LoggedIn(session)), emitted)

            // Updates still flow after recovery
            repo.clear()
            assertEquals(
                listOf(SessionState.LoggedOut, SessionState.LoggedIn(session), SessionState.LoggedOut),
                emitted,
            )
            job.cancel()
        }

    @Test
    fun storeDuringReadRetryIsEmittedImmediately() =
        runTest {
            val repo = SessionRepository(
                readSecureStorage = { throw IllegalStateException("Locked") },
                writeSecureStorage = { key, value ->
                    inMemoryStorage = value
                    WriteResult.Updated(key)
                },
                deleteSecureStorage = { key -> DeleteResult.Deleted(key) },
                json = json,
                readRetryDelay = 5.seconds,
            )

            val emitted = mutableListOf<SessionState>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                repo.sessionState.collect { emitted.add(it) }
            }

            repo.store(session)

            assertEquals(listOf(SessionState.LoggedOut, SessionState.LoggedIn(session)), emitted)
            job.cancel()
        }

    @Test
    fun getSessionReturnsNullWhenLoggedOut() =
        runTest {
            assertNull(repository.getSession())
        }

    @Test
    fun getSessionReturnsNullWhenReadFails() =
        runTest {
            shouldFailRead = true
            assertNull(repository.getSession())
        }

    @Test
    fun getSessionReturnsStoredSession() =
        runTest {
            repository.store(session)
            assertEquals(session, repository.getSession())
        }

    @Test
    fun requireSessionFailsWhenLoggedOut() =
        runTest {
            val result = repository.requireSession()
            assertTrue((result as Failure).reason is AuthException.NotLoggedIn)
        }

    @Test
    fun requireSessionSucceedsWhenLoggedIn() =
        runTest {
            repository.store(session)
            assertEquals(session, (repository.requireSession() as Success).value)
        }
}

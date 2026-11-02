package org.ooni.probe.data.repositories

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    fun sessionStateEmitsErrorWhenReadFails() =
        runTest {
            shouldFailRead = true
            val state = repository.sessionState.first()
            assertTrue(state is SessionState.Error)
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

package org.ooni.probe.domain.descriptors

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.SessionState
import org.ooni.probe.shared.toLocalDateTime
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class IsDescriptorEditableTest {
    private val session = AuthSession(
        sessionToken = "token",
        emailAddress = "author@example.org",
        role = "user",
        loginTime = Instant.parse("2025-01-01T00:00:00Z"),
    )

    @Test
    fun returnsTrueWhenAuthorMatchesAndNotExpired() =
        runTest {
            val descriptor = DescriptorFactory.buildInstalledModel(
                author = "author@example.org",
                netTests = listOf(NetTest(TestType.WebConnectivity)),
                expirationDate = now().plus(10.days).toLocalDateTime(),
            )
            val subject = IsDescriptorEditable(
                observeSessionState = { flowOf(SessionState.LoggedIn(session)) },
            )

            assertTrue(subject(descriptor).first())
        }

    @Test
    fun returnsFalseWhenDescriptorIsExpired() =
        runTest {
            val descriptor = DescriptorFactory.buildInstalledModel(
                author = "author@example.org",
                netTests = listOf(NetTest(TestType.WebConnectivity)),
                expirationDate = now().minus(10.days).toLocalDateTime(),
            )
            val subject = IsDescriptorEditable(
                observeSessionState = { flowOf(SessionState.LoggedIn(session)) },
            )

            assertFalse(subject(descriptor).first())
        }

    @Test
    fun returnsFalseWhenLoggedOut() =
        runTest {
            val descriptor = DescriptorFactory.buildInstalledModel(
                author = "author@example.org",
                netTests = listOf(NetTest(TestType.WebConnectivity)),
            )
            val subject = IsDescriptorEditable(
                observeSessionState = { flowOf(SessionState.LoggedOut) },
            )

            assertFalse(subject(descriptor).first())
        }

    @Test
    fun returnsFalseWhenAuthorMismatches() =
        runTest {
            val descriptor = DescriptorFactory.buildInstalledModel(
                author = "other@example.org",
                netTests = listOf(NetTest(TestType.WebConnectivity)),
            )
            val subject = IsDescriptorEditable(
                observeSessionState = { flowOf(SessionState.LoggedIn(session)) },
            )

            assertFalse(subject(descriptor).first())
        }

    private fun now() = Clock.System.now()
}

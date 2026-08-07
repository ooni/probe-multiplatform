package org.ooni.probe.domain.proxy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.ProxyOption
import org.ooni.testing.factories.PassportHttpResponseFactory
import org.ooni.testing.factories.ProxyOptionFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class TestProxyTest {
    @Test
    fun proxyToTestIsAvailable() =
        runTest {
            val subject = TestProxy(
                getProxyOption = { flowOf(ProxyOption.None) },
                passportGet = suspend { _, _ -> Success(PassportHttpResponseFactory.successful()) },
                backgroundContext = Dispatchers.Default,
                ooniApiBaseUrl = "https://api.ooni.io",
            )

            assertEquals(
                listOf(TestProxy.State.Testing, TestProxy.State.Available),
                subject(ProxyOptionFactory.custom()).toList(),
            )
        }

    @Test
    fun proxyToTestIsNotAvailable() =
        runTest {
            val subject = TestProxy(
                getProxyOption = { flowOf(ProxyOption.None) },
                passportGet = suspend { _, _ -> Success(PassportHttpResponseFactory.error()) },
                backgroundContext = Dispatchers.Default,
                ooniApiBaseUrl = "https://api.ooni.io",
            )

            assertEquals(
                listOf(TestProxy.State.Testing, TestProxy.State.Unavailable),
                subject(ProxyOptionFactory.custom()).toList(),
            )
        }

    @Test
    fun doesNotTestIfNoProxyIsProvidedAndCurrentIsNone() =
        runTest {
            val subject = TestProxy(
                getProxyOption = { flowOf(ProxyOption.None) },
                passportGet = suspend { _, _ -> fail("Should not test with a None proxy option") },
                backgroundContext = Dispatchers.Default,
                ooniApiBaseUrl = "https://api.ooni.io",
            )

            assertEquals(
                listOf(TestProxy.State.Available),
                subject(null).toList(),
            )
        }

    @Test
    fun testsCurrentProxyOptionIfNoProxyIsProvided() =
        runTest {
            val proxy = ProxyOptionFactory.custom()
            val subject = TestProxy(
                getProxyOption = { flowOf(proxy) },
                passportGet = suspend { _, proxyProvided ->
                    assertEquals(proxy.value, proxyProvided)
                    Success(PassportHttpResponseFactory.successful())
                },
                backgroundContext = Dispatchers.Default,
                ooniApiBaseUrl = "https://api.ooni.io",
            )

            assertEquals(
                listOf(TestProxy.State.Testing, TestProxy.State.Available),
                subject(null).toList(),
            )
        }
}

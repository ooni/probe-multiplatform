package org.ooni.passport

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.passport.models.SubmitCredentialConfig
import org.ooni.probe.data.models.ProxyOption
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PassportHttpClientTest {
    @Test
    fun offlineFailsWithoutTouchingTheBridge() =
        runTest {
            val bridge = RecordingBridge()
            val subject = subject(bridge, isOnline = false)

            val result = subject.get("https://example.org")

            assertIs<PassportException.Offline>(result.getError())
            assertEquals(0, bridge.callCount)
        }

    @Test
    fun offlineGatesEveryMethod() =
        runTest {
            val bridge = RecordingBridge()
            val subject = subject(bridge, isOnline = false)

            assertIs<PassportException.Offline>(subject.get("https://example.org").getError())
            assertIs<PassportException.Offline>(subject.post("https://example.org", "{}").getError())
            assertIs<PassportException.Offline>(
                subject.userAuthRegister("https://example.org", "params", "v1").getError(),
            )
            assertIs<PassportException.Offline>(
                subject
                    .userAuthSubmit(
                        url = "https://example.org",
                        content = "{}",
                        probeCc = "IT",
                        probeAsn = "AS1",
                        credentialConfig = null,
                    ).getError(),
            )

            assertEquals(0, bridge.callCount)
        }

    @Test
    fun offlineNeverEvenResolvesTheProxy() =
        runTest {
            val bridge = RecordingBridge()
            var proxyResolved = false
            val subject = PassportHttpClient(
                passportGet = bridge,
                passportPost = bridge,
                passportAuthRegister = bridge,
                passportAuthSubmit = bridge,
                getProxyOption = {
                    proxyResolved = true
                    flowOf(ProxyOption.None)
                },
                backgroundContext = RecordingDispatcher(),
                isOnline = { false },
            )

            subject.get("https://example.org")

            assertEquals(false, proxyResolved)
        }

    /**
     * The bridge call blocks its thread for the whole request, so it must never run on the
     * caller's dispatcher. A dispatcher that counts its dispatches proves the hand-off happened.
     */
    @Test
    fun onlineDispatchesOnTheInjectedBackgroundContext() =
        runTest {
            val bridge = RecordingBridge()
            val dispatcher = RecordingDispatcher()
            val subject = subject(bridge, isOnline = true, backgroundContext = dispatcher)

            val result = subject.get("https://example.org")

            assertIs<Success<PassportHttpResponse>>(result)
            assertEquals(1, bridge.callCount)
            assertTrue(
                dispatcher.dispatchCount > 0,
                "bridge call must be dispatched onto the injected background context",
            )
        }

    @Test
    fun offlineDoesNotDispatchAtAll() =
        runTest {
            val dispatcher = RecordingDispatcher()
            val subject = subject(RecordingBridge(), isOnline = false, backgroundContext = dispatcher)

            subject.get("https://example.org")

            assertEquals(0, dispatcher.dispatchCount)
        }

    @Test
    fun timeoutDefaultsToDefaultSecondsAndCanBeOverriddenPerCall() =
        runTest {
            val bridge = RecordingBridge()
            val subject = subject(bridge, isOnline = true)

            subject.get("https://example.org")
            assertEquals(PassportTimeouts.DEFAULT_SECONDS, bridge.lastTimeout)

            subject.get("https://example.org", timeout = PassportTimeouts.PREFETCH_SECONDS)
            assertEquals(PassportTimeouts.PREFETCH_SECONDS, bridge.lastTimeout)

            subject.post("https://example.org", "{}", timeout = PassportTimeouts.PREFETCH_SECONDS)
            assertEquals(PassportTimeouts.PREFETCH_SECONDS, bridge.lastTimeout)

            subject.userAuthSubmit(
                url = "https://example.org",
                content = "{}",
                probeCc = "IT",
                probeAsn = "AS1",
                credentialConfig = null,
            )
            assertEquals(PassportTimeouts.DEFAULT_SECONDS, bridge.lastTimeout)
        }

    @Test
    fun selectedProxyIsUsedUnlessOverridden() =
        runTest {
            val bridge = RecordingBridge()
            val subject = subject(bridge, isOnline = true, proxy = ProxyOption.Psiphon)

            subject.get("https://example.org")
            assertEquals(ProxyOption.Psiphon.value, bridge.lastProxy)

            subject.get("https://example.org", proxyOverride = "socks5://127.0.0.1:9050")
            assertEquals("socks5://127.0.0.1:9050", bridge.lastProxy)
        }

    @Test
    fun emptyProxyOptionResolvesToNoProxy() =
        runTest {
            val bridge = RecordingBridge()
            val subject = subject(bridge, isOnline = true, proxy = ProxyOption.None)

            subject.get("https://example.org")

            assertNull(bridge.lastProxy)
        }

    private fun subject(
        bridge: RecordingBridge,
        isOnline: Boolean,
        proxy: ProxyOption = ProxyOption.None,
        backgroundContext: CoroutineContext = RecordingDispatcher(),
    ) = PassportHttpClient(
        passportGet = bridge,
        passportPost = bridge,
        passportAuthRegister = bridge,
        passportAuthSubmit = bridge,
        getProxyOption = { flowOf(proxy) },
        backgroundContext = backgroundContext,
        isOnline = { isOnline },
    )

    /** Runs blocks inline but still reports every dispatch, so offloading is observable. */
    private class RecordingDispatcher : CoroutineDispatcher() {
        var dispatchCount = 0
            private set

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatchCount++
            block.run()
        }
    }

    private class RecordingBridge :
        PassportGet,
        PassportPost,
        PassportAuthRegister,
        PassportAuthSubmit {
        var callCount = 0
            private set
        var lastTimeout: Float? = null
            private set
        var lastProxy: String? = null
            private set

        private fun record(
            proxy: String?,
            timeout: Float?,
        ) {
            callCount++
            lastProxy = proxy
            lastTimeout = timeout
        }

        override fun get(
            url: String,
            headers: List<PassportBridge.KeyValue>,
            query: List<PassportBridge.KeyValue>,
            proxy: String?,
            timeout: Float?,
        ): Result<PassportHttpResponse, PassportException> {
            record(proxy, timeout)
            return Success(response())
        }

        override fun post(
            url: String,
            headers: List<PassportBridge.KeyValue>,
            payload: String,
            proxy: String?,
            timeout: Float?,
        ): Result<PassportHttpResponse, PassportException> {
            record(proxy, timeout)
            return Success(response())
        }

        override fun userAuthRegister(
            url: String,
            publicParams: String,
            manifestVersion: String,
            proxy: String?,
            timeout: Float?,
        ): Result<CredentialResponse, PassportException> {
            record(proxy, timeout)
            return Failure(PassportException.Other("not exercised"))
        }

        override fun userAuthSubmit(
            url: String,
            content: String,
            probeCc: String,
            probeAsn: String,
            proxy: String?,
            timeout: Float?,
            credentialConfig: SubmitCredentialConfig?,
        ): Result<CredentialResponse, PassportException> {
            record(proxy, timeout)
            return Failure(PassportException.Other("not exercised"))
        }

        private fun response() =
            PassportHttpResponse(
                statusCode = 200,
                version = "HTTP/1.1",
                headersListText = emptyList(),
                bodyText = "ok",
            )
    }
}

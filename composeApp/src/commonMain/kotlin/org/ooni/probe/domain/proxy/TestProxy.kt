package org.ooni.probe.domain.proxy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import org.ooni.engine.models.Result
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.ProxyOption
import kotlin.coroutines.CoroutineContext

class TestProxy(
    private val getProxyOption: () -> Flow<ProxyOption>,
    private val passportGet: suspend (url: String, proxyOverride: String?) -> Result<PassportHttpResponse, PassportException>,
    private val backgroundContext: CoroutineContext,
) {
    // If no proxy is provided, we test the current selected option
    operator fun invoke(proxyToTest: ProxyOption.Custom? = null): Flow<State> =
        channelFlow {
            val proxyToTest = proxyToTest ?: getProxyOption().first()
            // No need to test if there's no proxy selected
            if (proxyToTest == ProxyOption.None) {
                send(State.Available)
                return@channelFlow
            }

            send(State.Testing)

            val response = passportGet(
                "${OrganizationConfig.ooniApiBaseUrl}/health",
                proxyToTest.value.takeIf { it.isNotEmpty() },
            )
            if (response.get()?.isSuccessful == true) {
                send(State.Available)
            } else {
                send(State.Unavailable)
            }
        }.flowOn(backgroundContext)

    enum class State {
        Testing,
        Unavailable,
        Available,
    }
}

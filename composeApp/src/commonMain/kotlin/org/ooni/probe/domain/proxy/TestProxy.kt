package org.ooni.probe.domain.proxy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import org.ooni.engine.models.Result
import org.ooni.passport.PassportBridge.KeyValue
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.domain.credentials.CredentialsConstants
import kotlin.coroutines.CoroutineContext

class TestProxy(
    private val passportGet: (
        url: String,
        headers: List<KeyValue>,
        query: List<KeyValue>,
        proxy: String?,
        timeout: Float?,
    ) -> Result<PassportHttpResponse, PassportException>,
    private val getProxyOption: () -> Flow<ProxyOption>,
    private val backgroundContext: CoroutineContext,
) {
    operator fun invoke(proxyToTest: ProxyOption? = null): Flow<State> =
        channelFlow {
            send(State.Testing)

            val proxy = proxyToTest ?: getProxyOption().first()
            if (proxy == ProxyOption.None) {
                send(State.Available)
                return@channelFlow
            }

            val response = passportGet(
                "${OrganizationConfig.ooniApiBaseUrl}/health",
                emptyList(),
                emptyList(),
                proxy.value.takeIf { it.isNotEmpty() },
                CredentialsConstants.HTTP_TIMEOUT_SECONDS,
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

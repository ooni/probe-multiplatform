package org.ooni.probe.domain.proxy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import org.ooni.engine.models.Result
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.ProxyOption
import kotlin.coroutines.CoroutineContext

class TestProxy(
    private val passportGet: suspend (url: String, proxyOverride: String?) -> Result<PassportHttpResponse, PassportException>,
    private val backgroundContext: CoroutineContext,
) {
    operator fun invoke(proxyToTest: ProxyOption.Custom? = null): Flow<State> =
        channelFlow {
            send(State.Testing)

            val response = passportGet(
                "${OrganizationConfig.ooniApiBaseUrl}/health",
                proxyToTest?.value?.takeIf { it.isNotEmpty() },
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

package org.ooni.probe.domain.proxy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.ProxyOption
import kotlin.coroutines.CoroutineContext

class TestProxy(
    val httpDo: suspend (String, String, TaskOrigin) -> Result<String?, MkException>,
    private val backgroundContext: CoroutineContext,
) {
    operator fun invoke(proxy: ProxyOption.Custom): Flow<State> =
        channelFlow {
            send(State.Testing)

            if (httpDo("GET", "https://api.ooni.org/health", TaskOrigin.OoniRun) is Success) {
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

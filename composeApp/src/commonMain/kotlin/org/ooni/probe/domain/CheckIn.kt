package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import org.ooni.engine.Engine
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.models.Result
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.UrlModel

class CheckIn(
    private val engineCheckIn: suspend (TaskOrigin) -> Result<OonimkallBridge.CheckInResults, Engine.MkException>,
    private val storeUrlsByUrl: suspend (List<UrlModel>) -> List<UrlModel>,
) {
    suspend operator fun invoke(taskOrigin: TaskOrigin): Result<List<UrlModel>, Failure> =
        engineCheckIn(taskOrigin)
            .map { results ->
                val urls = results.urls.map { it.toModel() }
                storeUrlsByUrl(urls)
            }.mapError { Failure(it) }
            .onFailure { Logger.w("Could not check in", it) }

    private fun OonimkallBridge.UrlInfo.toModel() =
        UrlModel(
            url = url,
            countryCode = countryCode,
            category = WebConnectivityCategory.fromCode(categoryCode),
        )

    class Failure(
        cause: Engine.MkException,
    ) : Exception(cause)
}

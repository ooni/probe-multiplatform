package org.ooni.probe.domain

import org.ooni.engine.Engine
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.models.Result
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.UrlModel

class DownloadUrls(
    private val engineCheckIn: suspend (TaskOrigin) -> Result<OonimkallBridge.CheckInResults, Engine.MkException>,
    private val storeUrlsByUrl: suspend (List<UrlModel>) -> List<UrlModel>,
) {
    suspend operator fun invoke(taskOrigin: TaskOrigin): Result<List<UrlModel>, Engine.MkException> =
        engineCheckIn(taskOrigin)
            .map { results ->
                val urls = results.urls.map { it.toModel() }
                storeUrlsByUrl(urls)
            }

    private fun OonimkallBridge.UrlInfo.toModel() =
        UrlModel(
            url = url,
            countryCode = countryCode,
            category = WebConnectivityCategory.fromCode(categoryCode),
        )
}

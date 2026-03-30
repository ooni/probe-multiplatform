package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.engine.models.TaskOrigin
import org.ooni.passport.PassportPost
import org.ooni.passport.models.CheckInRequest
import org.ooni.passport.models.CheckInResponse
import org.ooni.probe.config.BuildTypeDefaults
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.UrlModel
import org.ooni.probe.shared.monitoring.Instrumentation

class CheckIn(
    private val passportPost: PassportPost,
    private val buildCheckInRequest: suspend (TaskOrigin) -> CheckInRequest,
    private val json: Json,
    private val setPreferenceByKey: suspend (SettingsKey, Any?) -> Unit,
    private val storeUrlsByUrl: suspend (List<UrlModel>) -> List<UrlModel>,
) {
    suspend operator fun invoke(taskOrigin: TaskOrigin): Result<CheckInResponse, Unsuccessful> {
        val request = buildCheckInRequest(taskOrigin)

        return Instrumentation.withTransaction(
            operation = "CheckIn",
            data = mapOf(
                "charging" to request.charging,
                "onWifi" to request.onWifi.toString(),
                "taskOrigin" to taskOrigin.value,
                "categoriesCount" to request.webConnectivity.categoryCodes.size,
            ),
        ) {
            passportPost
                .post(
                    url = "${BuildTypeDefaults.ooniApiBaseUrl}/api/v1/check-in",
                    headers = emptyList(),
                    payload = json.encodeToString(request),
                ).mapError { Unsuccessful(it) }
                .onFailure { Logger.w("Could not check-in", it) }
                .flatMap { result ->
                    if (!result.isSuccessful || result.bodyText == null) {
                        Logger.w("Check-in response unsuccessful: $result")
                        return@flatMap Failure(Unsuccessful(null))
                    }

                    val response = try {
                        json.decodeFromString<CheckInResponse>(result.bodyText)
                    } catch (e: Exception) {
                        Logger.w("Could not parse check-in response", e)
                        return@flatMap Failure(Unsuccessful(e))
                    }

                    setPreferenceByKey(
                        SettingsKey.DISABLED_TESTS,
                        response.disabledTests.map { it.preferenceKey },
                    )
                    val urls = response.urls
                    storeUrlsByUrl(urls)
                    Success(response)
                }
        }
    }

    class Unsuccessful(
        cause: Exception?,
    ) : Exception(cause)
}

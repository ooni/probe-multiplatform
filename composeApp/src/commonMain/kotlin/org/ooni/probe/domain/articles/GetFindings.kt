package org.ooni.probe.domain.articles

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.PassportBridge.KeyValue
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.domain.credentials.CredentialsConstants
import org.ooni.probe.shared.toLocalDateTime
import kotlin.time.Instant

class GetFindings(
    val passportGet: (
        url: String,
        headers: List<KeyValue>,
        query: List<KeyValue>,
        proxy: String?,
        timeout: Float?,
    ) -> Result<PassportHttpResponse, PassportException>,
    val getProxyOption: () -> Flow<ProxyOption>,
    val json: Json,
) : RefreshArticles.Source {
    override suspend operator fun invoke(): Result<List<ArticleModel>, Exception> {
        val proxy = getProxyOption().first().value.takeIf { it.isNotEmpty() }
        return passportGet(
            "${OrganizationConfig.ooniApiBaseUrl}/api/v1/incidents/search",
            emptyList(),
            emptyList(),
            proxy,
            CredentialsConstants.HTTP_TIMEOUT_SECONDS,
        ).mapError { it as Exception }
            .flatMap { response ->
                if (!response.isSuccessful) {
                    return@flatMap Failure(Exception("Unsuccessful response (status=${response.statusCode})"))
                }
                if (response.bodyText.isNullOrBlank()) return@flatMap Failure(Exception("Empty response"))

                val wrapper = try {
                    json.decodeFromString<Wrapper>(response.bodyText)
                } catch (e: Exception) {
                    return@flatMap Failure(e)
                }

                Success(wrapper.incidents?.mapNotNull { it.toArticle() }.orEmpty())
            }
    }

    private fun Wrapper.Incident.toArticle() =
        run {
            ArticleModel(
                url = id?.let { ArticleModel.Url("${OrganizationConfig.explorerUrl}/findings/$it") }
                    ?: return@run null,
                title = title ?: return@run null,
                source = ArticleModel.Source.Finding,
                description = shortDescription,
                imageUrl = null,
                time = createTime?.toLocalDateTime() ?: return@run null,
            )
        }

    @OptIn(FormatStringsInDatetimeFormats::class)
    private fun String.toLocalDateTime(): LocalDateTime? = Instant.parseOrNull(this)?.toLocalDateTime()

    @Serializable
    data class Wrapper(
        @SerialName("incidents")
        val incidents: List<Incident>?,
    ) {
        @Serializable
        data class Incident(
            @SerialName("id") val id: String?,
            @SerialName("title") val title: String?,
            @SerialName("short_description") val shortDescription: String?,
            @SerialName("create_time") val createTime: String?,
        )
    }
}

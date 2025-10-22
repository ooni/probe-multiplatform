package org.ooni.probe.domain.articles

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.shared.toLocalDateTime
import kotlin.time.Instant

class GetFindings(
    val httpDo: suspend (String, String, TaskOrigin) -> Result<String?, MkException>,
    val json: Json,
) : RefreshArticles.Source {
    override suspend operator fun invoke(): Result<List<ArticleModel>, Exception> {
        return httpDo("GET", "https://api.ooni.org/api/v1/incidents/search", TaskOrigin.OoniRun)
            .mapError { Exception("Failed to get findings", it) }
            .flatMap { response ->
                if (response.isNullOrBlank()) return@flatMap Failure(Exception("Empty response"))

                val wrapper = try {
                    json.decodeFromString<Wrapper>(response)
                } catch (e: Exception) {
                    return@flatMap Failure(Exception("Could not parse indidents API response", e))
                }

                Success(wrapper.incidents?.mapNotNull { it.toArticle() }.orEmpty())
            }
    }

    private fun Wrapper.Incident.toArticle() =
        run {
            ArticleModel(
                url = id?.let { ArticleModel.Url("https://explorer.ooni.org/findings/$it") }
                    ?: return@run null,
                title = title ?: return@run null,
                source = ArticleModel.Source.Finding,
                description = shortDescription,
                time = createTime?.toLocalDateTime() ?: return@run null,
            )
        }

    @OptIn(FormatStringsInDatetimeFormats::class)
    private fun String.toLocalDateTime(): LocalDateTime? = Instant.parse(this).toLocalDateTime()

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

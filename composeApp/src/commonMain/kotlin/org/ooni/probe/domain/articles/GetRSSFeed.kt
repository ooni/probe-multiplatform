package org.ooni.probe.domain.articles

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.parse
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.PassportBridge.KeyValue
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.domain.credentials.CredentialsConstants
import org.ooni.probe.shared.toLocalDateTime
import kotlin.time.Instant

class GetRSSFeed(
    val passportGet: (
        url: String,
        headers: List<KeyValue>,
        query: List<KeyValue>,
        proxy: String?,
        timeout: Float?,
    ) -> Result<PassportHttpResponse, PassportException>,
    val getProxyOption: () -> Flow<ProxyOption>,
    val url: String,
    val source: ArticleModel.Source,
) : RefreshArticles.Source {
    override suspend operator fun invoke(): Result<List<ArticleModel>, Exception> {
        val proxy = getProxyOption().first().value.takeIf { it.isNotEmpty() }
        return passportGet(url, emptyList(), emptyList(), proxy, CredentialsConstants.HTTP_TIMEOUT_SECONDS)
            .mapError { it as Exception }
            .flatMap { response ->
                if (!response.isSuccessful) {
                    return@flatMap Failure(Exception("Unsuccessful response (status=${response.statusCode})"))
                }
                if (response.bodyText.isNullOrBlank()) return@flatMap Failure(Exception("Empty response"))

                val rss = try {
                    xml().decodeFromString<Rss>(response.bodyText)
                } catch (e: Exception) {
                    return@flatMap Failure(e)
                }

                Success(
                    rss.channel
                        ?.items
                        ?.mapNotNull { it.toArticle() }
                        .orEmpty(),
                )
            }
    }

    private fun Rss.Item.toArticle() =
        run {
            ArticleModel(
                url = ArticleModel.Url(link ?: return@run null),
                title = title ?: return@run null,
                source = source,
                description = description,
                imageUrl = content?.url,
                time = pubDate?.toLocalDateTime() ?: return@run null,
            )
        }

    @OptIn(FormatStringsInDatetimeFormats::class)
    private fun String.toLocalDateTime(): LocalDateTime? =
        Instant
            .parse(
                this,
                DateTimeComponents.Format {
                    dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                    chars(", ")
                    day()
                    chars(" ")
                    monthName(MonthNames.ENGLISH_ABBREVIATED)
                    chars(" ")
                    byUnicodePattern("yyyy HH:mm:ss Z")
                },
            ).toLocalDateTime()

    companion object Companion {
        private fun xml() =
            XML {
                defaultPolicy {
                    ignoreUnknownChildren()
                }
            }
    }

    @Serializable
    @XmlSerialName("rss")
    data class Rss(
        @XmlSerialName("channel")
        @XmlElement
        val channel: Channel?,
    ) {
        @Serializable
        data class Channel(
            @XmlSerialName("item")
            @XmlElement
            val items: List<Item>?,
        )

        @Serializable
        data class Item(
            @XmlSerialName("title")
            @XmlElement
            val title: String?,
            @XmlSerialName("link")
            @XmlElement
            val link: String?,
            @XmlSerialName("description")
            @XmlElement
            val description: String?,
            @XmlSerialName("pubDate")
            @XmlElement
            val pubDate: String?,
            @XmlSerialName("content", namespace = "http://search.yahoo.com/mrss/")
            @XmlElement
            val content: MediaContent?,
        )

        @Serializable
        data class MediaContent(
            @XmlSerialName("url")
            @XmlElement(false)
            val url: String?,
        )
    }
}

package org.ooni.probe.data.models

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import org.ooni.probe.shared.today

data class ArticleModel(
    val url: Url,
    val title: String,
    val description: String?,
    val source: Source,
    val time: LocalDateTime,
) {
    data class Url(
        val value: String,
    )

    val isRecent get() = time.date >= LocalDate.today().minus(7, DateTimeUnit.DAY)

    enum class Source(
        val value: String,
    ) {
        Blog("blog"),
        Finding("finding"),
        Report("report"),
        ;

        companion object {
            fun fromValue(value: String) = entries.firstOrNull { it.value == value }
        }
    }
}

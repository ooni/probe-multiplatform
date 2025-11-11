package org.ooni.testing.factories

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.shared.today
import kotlin.random.Random

object ArticleModelFactory {
    fun build(
        url: ArticleModel.Url = ArticleModel.Url("https://example.org/${Random.nextInt()}"),
        title: String = "Title",
        time: LocalDateTime = LocalDate.today().atTime(0, 0),
        description: String? = null,
        imageUrl: String? = null,
        source: ArticleModel.Source = ArticleModel.Source.Blog,
    ) = ArticleModel(
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        time = time,
        source = source,
    )
}

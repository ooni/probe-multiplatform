package org.ooni.probe.domain.articles

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.data.models.SettingsKey
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class RefreshArticles(
    val sources: List<Source>,
    val refreshArticlesInDatabase: suspend (List<ArticleModel>) -> Unit,
    val getPreference: (SettingsKey) -> Flow<Any?>,
    val setPreference: suspend (SettingsKey, Any) -> Unit,
) {
    fun interface Source {
        suspend operator fun invoke(): Result<List<ArticleModel>, Exception>
    }

    suspend operator fun invoke() {
        if (!OrganizationConfig.hasOoniNews) return

        val lastCheck = (getPreference(SettingsKey.LAST_ARTICLES_REFRESH).first() as? Long)
            ?.let { Instant.fromEpochSeconds(it) }
        if (lastCheck != null && Clock.System.now() - lastCheck < MIN_INTERVAL) return

        val responses = sources
            .map {
                coroutineScope { async { it() } }
            }.awaitAll()

        responses.forEach { response ->
            response.onFailure {
                Logger.w("Failed to get article source", it)
            }
        }

        if (responses.all { it is Success }) {
            refreshArticlesInDatabase(
                responses.mapNotNull { it.get() }.flatten(),
            )
        }

        setPreference(SettingsKey.LAST_ARTICLES_REFRESH, Clock.System.now().epochSeconds)
    }

    companion object {
        private val MIN_INTERVAL = 1.days
    }
}

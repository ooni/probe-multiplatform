package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ooni.probe.Database
import org.ooni.probe.data.Url
import org.ooni.probe.data.models.UrlModel

class UrlRepository(
    private val database: Database,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    fun list() =
        database.urlQueries
            .selectAll()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { list -> list.mapNotNull { it.toModel() } }

    suspend fun create(model: UrlModel) {
        withContext(backgroundDispatcher) {
            database.urlQueries.insert(
                id = model.id?.value,
                url = model.url,
                country_code = model.countryCode,
                category_code = model.categoryCode,
            )
        }
    }

    private fun Url.toModel(): UrlModel? {
        return UrlModel(
            id = UrlModel.Id(id),
            url = url ?: return null,
            countryCode = country_code,
            categoryCode = category_code,
        )
    }
}

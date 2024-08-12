package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ooni.probe.Database
import org.ooni.probe.data.TestDescriptor
import org.ooni.probe.data.models.InstalledTestDescriptorModel

class TestDescriptorRepository(
    private val database: Database,
    private val json: Json,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    fun list() =
        database.testDescriptorQueries
            .selectAll()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { list -> list.mapNotNull { it.toModel() } }

    suspend fun createOrIgnore(models: List<InstalledTestDescriptorModel>) {
        withContext(backgroundDispatcher) {
            database.transaction {
                models.forEach { model ->
                    database.testDescriptorQueries.insertOrIgnore(
                        runId = model.id.value,
                        name = model.name,
                        short_description = model.shortDescription,
                        description = model.description,
                        author = model.author,
                        nettests = json.encodeToString(model.netTests),
                        name_intl = json.encodeToString(model.nameIntl),
                        short_description_intl = json.encodeToString(model.shortDescriptionIntl),
                        description_intl = json.encodeToString(model.descriptionIntl),
                        icon = model.icon,
                        color = model.color,
                        animation = model.animation,
                        expiration_date = model.expirationDate?.toEpochMilliseconds(),
                        date_created = model.dateCreated?.toEpochMilliseconds(),
                        date_updated = model.dateUpdated?.toEpochMilliseconds(),
                        revision = model.revision,
                        previous_revision = null,
                        is_expired = if (model.isExpired) 1 else 0,
                        auto_update = if (model.autoUpdate) 1 else 0,
                    )
                }
            }
        }
    }

    private fun TestDescriptor.toModel(): InstalledTestDescriptorModel? {
        return InstalledTestDescriptorModel(
            id = runId?.let(InstalledTestDescriptorModel::Id) ?: return null,
            name = name.orEmpty(),
            shortDescription = short_description,
            description = description,
            author = author,
            netTests = nettests?.let(json::decodeFromString),
            nameIntl = name_intl?.let(json::decodeFromString),
            shortDescriptionIntl = short_description_intl?.let(json::decodeFromString),
            descriptionIntl = description_intl?.let(json::decodeFromString),
            icon = icon,
            color = color,
            animation = animation,
            expirationDate = expiration_date?.let(Instant::fromEpochMilliseconds),
            dateCreated = date_created?.let(Instant::fromEpochMilliseconds),
            dateUpdated = date_updated?.let(Instant::fromEpochMilliseconds),
            revision = revision,
            isExpired = is_expired == 1L,
            autoUpdate = auto_update == 1L,
        )
    }
}

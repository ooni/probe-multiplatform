package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ooni.engine.models.OONINetTest
import org.ooni.probe.Database
import org.ooni.probe.data.TestDescriptor
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.shared.toEpoch
import org.ooni.probe.shared.toLocalDateTime

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
                        nettests = model.netTests
                            ?.map { it.toOONI() }
                            ?.let { json.encodeToString(it) },
                        name_intl = json.encodeToString(model.nameIntl),
                        short_description_intl = json.encodeToString(model.shortDescriptionIntl),
                        description_intl = json.encodeToString(model.descriptionIntl),
                        icon = model.icon,
                        color = model.color,
                        animation = model.animation,
                        expiration_date = model.expirationDate?.toEpoch(),
                        date_created = model.dateCreated?.toEpoch(),
                        date_updated = model.dateUpdated?.toEpoch(),
                        revision = json.encodeToString(model.revision),
                        previous_revision = null,
                        is_expired = if (model.isExpired) 1 else 0,
                        auto_update = if (model.autoUpdate) 1 else 0,
                    )
                }
            }
        }
    }

    suspend fun deleteByRunId(runId: InstalledTestDescriptorModel.Id) {
        withContext(backgroundDispatcher) {
            database.testDescriptorQueries.deleteByRunId(runId.value)
        }
    }

    private fun TestDescriptor.toModel(): InstalledTestDescriptorModel? {
        return InstalledTestDescriptorModel(
            id = runId?.let(InstalledTestDescriptorModel::Id) ?: return null,
            name = name.orEmpty(),
            shortDescription = short_description,
            description = description,
            author = author,
            netTests = nettests
                ?.let { json.decodeFromString<List<OONINetTest>>(it) }
                ?.map { NetTest.fromOONI(it) },
            nameIntl = name_intl?.let(json::decodeFromString),
            shortDescriptionIntl = short_description_intl?.let(json::decodeFromString),
            descriptionIntl = description_intl?.let(json::decodeFromString),
            icon = icon,
            color = color,
            animation = animation,
            expirationDate = expiration_date?.toLocalDateTime(),
            dateCreated = date_created?.toLocalDateTime(),
            dateUpdated = date_updated?.toLocalDateTime(),
            revision = revision?.let { json.decodeFromString<List<String>>(it) },
            autoUpdate = auto_update == 1L,
        )
    }
}

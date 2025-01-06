package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.ooni.engine.models.OONINetTest
import org.ooni.probe.Database
import org.ooni.probe.data.TestDescriptor
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.toDb
import org.ooni.probe.shared.toLocalDateTime
import kotlin.coroutines.CoroutineContext

class TestDescriptorRepository(
    private val database: Database,
    private val json: Json,
    private val backgroundContext: CoroutineContext,
) {
    fun list(includeExpired: Boolean? = null) =
        database.testDescriptorQueries
            .selectAll(isExpired = includeExpired?.let { if (it) 1 else 0 })
            .asFlow()
            .mapToList(backgroundContext)
            .map { list -> list.mapNotNull { it.toModel() } }

    fun selectByRunIds(ids: List<InstalledTestDescriptorModel.Id>) =
        database.testDescriptorQueries
            .selectByRunIds(ids.map { it.value })
            .asFlow()
            .mapToList(backgroundContext)
            .map { list -> list.mapNotNull { it.toModel() } }

    suspend fun createOrIgnore(models: List<InstalledTestDescriptorModel>) {
        withContext(backgroundContext) {
            database.transaction {
                models.forEach { model ->
                    val installedModel = model.toDb(json = json)
                    database.testDescriptorQueries.insertOrIgnore(
                        runId = installedModel.runId,
                        name = installedModel.name,
                        short_description = installedModel.short_description,
                        description = installedModel.description,
                        author = installedModel.author,
                        nettests = installedModel.nettests,
                        name_intl = installedModel.name_intl,
                        short_description_intl = installedModel.short_description_intl,
                        description_intl = installedModel.description_intl,
                        icon = installedModel.icon,
                        color = installedModel.color,
                        animation = installedModel.animation,
                        expiration_date = installedModel.expiration_date,
                        date_created = installedModel.date_created,
                        date_updated = installedModel.date_updated,
                        revision = installedModel.revision,
                        previous_revision = null,
                        is_expired = installedModel.is_expired,
                        auto_update = installedModel.auto_update,
                    )
                }
            }
        }
    }

    suspend fun createOrUpdate(models: Set<InstalledTestDescriptorModel>) {
        withContext(backgroundContext) {
            database.transaction {
                models.forEach { model ->
                    val installedModel = model.toDb(json = json)
                    database.testDescriptorQueries.createOrUpdate(
                        runId = installedModel.runId,
                        name = installedModel.name,
                        short_description = installedModel.short_description,
                        description = installedModel.description,
                        author = installedModel.author,
                        nettests = installedModel.nettests,
                        name_intl = installedModel.name_intl,
                        short_description_intl = installedModel.short_description_intl,
                        description_intl = installedModel.description_intl,
                        icon = installedModel.icon,
                        color = installedModel.color,
                        animation = installedModel.animation,
                        expiration_date = installedModel.expiration_date,
                        date_created = installedModel.date_created,
                        date_updated = installedModel.date_updated,
                        revision = installedModel.revision,
                        previous_revision = null,
                        is_expired = installedModel.is_expired,
                        auto_update = installedModel.auto_update,
                    )
                }
            }
        }
    }

    suspend fun setAutoUpdate(
        runId: InstalledTestDescriptorModel.Id,
        autoUpdate: Boolean,
    ) {
        withContext(backgroundContext) {
            database.testDescriptorQueries.setAutoUpdate(auto_update = if (autoUpdate) 1 else 0, runId = runId.value)
        }
    }

    suspend fun deleteByRunId(runId: InstalledTestDescriptorModel.Id) {
        withContext(backgroundContext) {
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
            revisions = revision?.let {
                try {
                    json.decodeFromString<List<String>>(it)
                } catch (e: Exception) {
                    // Handle the exception, e.g., log it or return a default value
                    emptyList()
                }
            },
            autoUpdate = auto_update == 1L,
        )
    }
}

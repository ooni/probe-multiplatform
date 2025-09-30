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
    /**
     * Lists all entries in the database.
     *
     * Warning: this list will bring duplicated descriptors of different revisions.
     */
    fun listAll() =
        database.testDescriptorQueries
            .selectAll()
            .asFlow()
            .mapToList(backgroundContext)
            .map { list -> list.map { it.toModel() } }

    /**
     * Lists the latest revision of every installed descriptor in the database.
     */
    fun listLatest() =
        database.testDescriptorQueries
            .selectLatest()
            .asFlow()
            .mapToList(backgroundContext)
            .map { list -> list.map { it.toModel() } }

    fun listLatestByRunIds(ids: List<InstalledTestDescriptorModel.Id>) =
        database.testDescriptorQueries
            .selectLatestByRunIds(ids.map { it.value })
            .asFlow()
            .mapToList(backgroundContext)
            .map { list -> list.map { it.toModel() } }

    suspend fun createOrIgnore(models: List<InstalledTestDescriptorModel>) {
        withContext(backgroundContext) {
            database.transaction {
                models.forEach { model ->
                    val installedModel = model.toDb(json = json)
                    database.testDescriptorQueries.insertOrIgnore(
                        runId = installedModel.runId,
                        revision = installedModel.revision,
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
                        date_installed = installedModel.date_installed,
                        auto_update = installedModel.auto_update,
                        rejected_revision = installedModel.rejected_revision,
                    )
                }
            }
        }
    }

    suspend fun createOrUpdate(models: List<InstalledTestDescriptorModel>) {
        withContext(backgroundContext) {
            database.transaction {
                models.forEach { model ->
                    val installedModel = model.toDb(json = json)
                    database.testDescriptorQueries.createOrUpdate(
                        runId = installedModel.runId,
                        revision = installedModel.revision,
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
                        date_installed = installedModel.date_installed,
                        auto_update = installedModel.auto_update,
                        rejected_revision = installedModel.rejected_revision,
                    )
                    database.testDescriptorQueries.clearOldNetTests(
                        installedModel.runId,
                        installedModel.revision,
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
            database.testDescriptorQueries.setAutoUpdate(
                auto_update = if (autoUpdate) 1 else 0,
                runId = runId.value,
            )
        }
    }

    suspend fun updateRejectedRevision(
        runId: InstalledTestDescriptorModel.Id,
        rejectedRevision: Long?,
    ) {
        withContext(backgroundContext) {
            database.testDescriptorQueries.updateRejectedRevision(
                runId = runId.value,
                rejected_revision = rejectedRevision,
            )
        }
    }

    suspend fun deleteByRunId(runId: InstalledTestDescriptorModel.Id) {
        withContext(backgroundContext) {
            database.testDescriptorQueries.deleteByRunId(runId.value)
        }
    }

    private fun TestDescriptor.toModel() =
        InstalledTestDescriptorModel(
            id = InstalledTestDescriptorModel.Id(runId),
            revision = revision,
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
            dateInstalled = date_installed?.toLocalDateTime(),
            autoUpdate = auto_update == 1L,
            rejectedRevision = rejected_revision,
        )
}

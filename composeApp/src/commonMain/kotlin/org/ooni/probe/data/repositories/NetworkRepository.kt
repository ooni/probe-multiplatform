package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ooni.engine.models.NetworkType
import org.ooni.probe.Database
import org.ooni.probe.data.Network
import org.ooni.probe.data.models.NetworkModel
import kotlin.coroutines.CoroutineContext

class NetworkRepository(
    private val database: Database,
    private val backgroundContext: CoroutineContext,
) {
    /*
     If the model has an ID, only update.
     If the model does not have an ID, search if we already have an entry with the same values.
     If we do, return that ID, otherwise create a new entry.
     */
    suspend fun createIfNew(model: NetworkModel): NetworkModel.Id =
        withContext(backgroundContext) {
            database.transactionWithResult {
                if (model.id == null) {
                    database.networkQueries
                        .selectByValues(
                            network_name = model.networkName,
                            asn = model.asn,
                            country_code = model.countryCode,
                            network_type = model.networkType?.value,
                        ).executeAsOneOrNull()
                        ?.let { return@transactionWithResult NetworkModel.Id(it.id) }
                }

                database.networkQueries.insertOrReplace(
                    id = model.id?.value,
                    network_name = model.networkName,
                    asn = model.asn,
                    country_code = model.countryCode,
                    network_type = model.networkType?.value,
                )

                model.id
                    ?: NetworkModel.Id(
                        database.networkQueries.selectLastInsertedRowId().executeAsOne(),
                    )
            }
        }

    fun list() =
        database.networkQueries
            .selectAll()
            .asFlow()
            .mapToList(backgroundContext)
            .map { list -> list.map { it.toModel() } }

    suspend fun deleteWithoutResult() =
        withContext(backgroundContext) {
            database.networkQueries.deleteWithoutResult()
        }
}

fun Network.toModel(): NetworkModel =
    NetworkModel(
        id = NetworkModel.Id(id),
        networkName = network_name,
        asn = asn,
        countryCode = country_code,
        networkType = network_type?.let(NetworkType::fromValue),
    )

package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ooni.engine.models.NetworkType
import org.ooni.probe.Database
import org.ooni.probe.data.Network
import org.ooni.probe.data.models.NetworkModel

class NetworkRepository(
    private val database: Database,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    fun list() =
        database.networkQueries
            .selectAll()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { list -> list.map { it.toModel() } }

    suspend fun create(model: NetworkModel) {
        withContext(backgroundDispatcher) {
            database.networkQueries.insert(
                id = model.id?.value,
                network_name = model.networkName,
                ip = model.ip,
                asn = model.asn,
                country_code = model.countryCode,
                network_type = model.networkType?.value,
            )
        }
    }
}

fun Network.toModel(): NetworkModel =
    NetworkModel(
        id = NetworkModel.Id(id),
        networkName = network_name,
        ip = ip,
        asn = asn,
        countryCode = country_code,
        networkType = network_type?.let(NetworkType::fromValue),
    )

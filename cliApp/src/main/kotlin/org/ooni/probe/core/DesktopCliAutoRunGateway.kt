package org.ooni.probe.core

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import org.ooni.engine.models.NetworkType
import org.ooni.probe.Database
import org.ooni.probe.data.buildDatabaseDriver
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.data.models.toDescriptorItem
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.data.repositories.ResultRepository
import org.ooni.probe.domain.CheckAutoRunConstraints
import org.ooni.probe.domain.GetAutoRunSettings
import org.ooni.probe.domain.GetAutoRunSpecification
import java.io.File

/**
 * Builds the production desktop/JVM CLI autorun gateway.
 *
 * It is backed by the same SQLDelight + DataStore storage the other CLI gateways use (via
 * [CliStorageConfig]) plus the bundled bootstrap descriptors, and drives the canonical autorun
 * domain use cases. No engine/native runtime is required: [CliAutoRunGateway.status] only reads
 * preferences, stored results, and the bundled descriptor set.
 *
 * The current CLI probe context is desktop with an unknown network type and a charging battery, and
 * (matching the desktop app's `PlatformInfo`) neither the network type nor the battery state is
 * treated as known — so the Wi-Fi/charging constraints are not applied, while the VPN and
 * not-uploaded-limit constraints still gate the result exactly as the shared app does.
 */
fun buildDesktopCliAutoRunGateway(config: CliStorageConfig): CliAutoRunGateway = DesktopCliAutoRunGateway(config)

private class DesktopCliAutoRunGateway(
    config: CliStorageConfig,
) : CliAutoRunGateway {
    private val backgroundContext = Dispatchers.IO
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val driver: SqlDriver = buildDatabaseDriver(config.databaseDir)
    private val database = Database(driver)
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val descriptorDecoderJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val resultRepository = ResultRepository(database, backgroundContext)
    private val preferenceRepository = PreferenceRepository(JsonFilePreferencesDataStore(File(config.preferencesFile)))
    private val descriptorAssets: DescriptorAssetProvider = ClasspathDescriptorAssetProvider()

    private val getAutoRunSettings = GetAutoRunSettings(preferenceRepository::allSettings)

    // Desktop CLI shared-state stubs, mirroring DesktopCliRunGateway (network unknown, charging).
    private val checkAutoRunConstraints = CheckAutoRunConstraints(
        getAutoRunSettings = getAutoRunSettings::invoke,
        getNetworkType = { NetworkType.Unknown("unknown") },
        getBatteryState = { BatteryState.Charging },
        knownNetworkType = false,
        knownBatteryState = false,
        countResultsMissingUpload = resultRepository::countMissingUpload,
    )

    private val getAutoRunSpecification = GetAutoRunSpecification(
        getLatestDescriptors = {
            flow {
                emit(
                    BootstrapDescriptorDecoder(descriptorDecoderJson)
                        .decode(descriptorAssets, DescriptorAssetSet.cliDefault)
                        .map { it.toDescriptorItem() },
                )
            }
        },
        preferenceRepository = preferenceRepository,
    )

    override suspend fun status(): CliAutoRunStatus {
        val parameters = getAutoRunSettings().first()
        val enabled = parameters as? AutoRunParameters.Enabled
        val constraintsSatisfied = checkAutoRunConstraints()
        val spec = getAutoRunSpecification()
        return CliAutoRunStatus(
            enabled = enabled != null,
            wifiOnly = enabled?.wifiOnly ?: false,
            onlyWhileCharging = enabled?.onlyWhileCharging ?: false,
            constraintsSatisfied = constraintsSatisfied,
            descriptorCount = spec.tests.size,
            testCount = spec.tests.sumOf { it.netTests.size },
        )
    }

    override fun close() {
        runCatching { driver.close() }
        scope.cancel()
    }
}

package org.ooni.probe.core

import kotlinx.serialization.json.Json
import org.ooni.probe.data.models.Descriptor

interface CliCoreGateway

interface CliCoreGatewayDependencies : CliCoreGateway {
    val descriptorAssets: DescriptorAssetProvider
    val defaultDescriptorAssetSet: DescriptorAssetSet
    val nativeRuntimeBootstrap: NativeRuntimeBootstrap

    /** Loads and decodes the bundled bootstrap descriptors for [assetSet]. */
    suspend fun loadDescriptors(assetSet: DescriptorAssetSet): List<Descriptor> =
        BootstrapDescriptorDecoder(descriptorDecoderJson).decode(descriptorAssets, assetSet)

    companion object {
        private val descriptorDecoderJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}

interface CliGatewayFactory {
    fun createCliGateway(): CliCoreGateway

    /** Builds the CLI run/upload orchestration gateway for the given engine config. */
    fun createCliRunGateway(config: CliEngineConfig): CliRunGateway

    /** Builds the CLI geoip lookup gateway for the given engine config. */
    fun createCliGeoIpGateway(config: CliEngineConfig): CliGeoIpGateway

    /** Builds the CLI autorun status gateway for the given storage config. */
    fun createCliAutoRunGateway(config: CliStorageConfig): CliAutoRunGateway

    /** Builds the CLI reset gateway (scoped secure-storage clearing) for the given engine config. */
    fun createCliResetGateway(config: CliEngineConfig): CliResetGateway
}

interface NativeRuntimeBootstrap {
    fun configure(): NativeRuntimeBootstrapResult
}

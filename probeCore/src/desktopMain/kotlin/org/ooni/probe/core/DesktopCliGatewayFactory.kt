package org.ooni.probe.core

object DesktopCliGatewayFactory : CliGatewayFactory {
    override fun createCliGateway(): CliCoreGateway = DesktopCliCoreGateway

    override fun createCliRunGateway(config: CliEngineConfig): CliRunGateway = buildDesktopCliRunGateway(config)

    override fun createCliGeoIpGateway(config: CliEngineConfig): CliGeoIpGateway = buildDesktopCliGeoIpGateway(config)

    override fun createCliAutoRunGateway(config: CliStorageConfig): CliAutoRunGateway = buildDesktopCliAutoRunGateway(config)

    override fun createCliResetGateway(config: CliEngineConfig): CliResetGateway = buildDesktopCliResetGateway(config)
}

private object DesktopCliCoreGateway : CliCoreGatewayDependencies {
    override val descriptorAssets: DescriptorAssetProvider = ClasspathDescriptorAssetProvider()
    override val defaultDescriptorAssetSet: DescriptorAssetSet = DescriptorAssetSet.cliDefault
    override val nativeRuntimeBootstrap: NativeRuntimeBootstrap = DesktopNativeRuntimeBootstrap
}

class ClasspathDescriptorAssetProvider(
    private val classLoader: ClassLoader = ClasspathDescriptorAssetProvider::class.java.classLoader,
) : DescriptorAssetProvider {
    override suspend fun load(assetSet: DescriptorAssetSet): String {
        val resource = "assets/descriptors/${assetSet.name.lowercase()}.json"
        return classLoader
            .getResourceAsStream(resource)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Missing descriptor asset: $resource")
    }
}

object DesktopNativeRuntimeBootstrap : NativeRuntimeBootstrap {
    override fun configure(): NativeRuntimeBootstrapResult = configureBundledNativeLibraries()
}

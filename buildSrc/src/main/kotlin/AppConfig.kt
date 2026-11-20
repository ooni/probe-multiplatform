/**
 * Configuration data class for different app variants.
 */
data class AppConfig(
    val appId: String,
    val appName: String,
    val folder: String,
    val supportsOoniRun: Boolean = false,
    val supportedLanguages: List<String>,
)

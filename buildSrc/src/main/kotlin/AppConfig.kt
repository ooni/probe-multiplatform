/**
 * Configuration data class for different app variants.
 */
data class AppConfig(
    val appId: String,
    val appName: String,
    val folder: String,
    /** Prefix for on-device storage names, e.g. "<baseSoftwareName>_secure_prefs". Must match OrganizationConfig.baseSoftwareName. */
    val baseSoftwareName: String,
    val supportsOoniRun: Boolean = false,
    val supportedLanguages: List<String>,
)

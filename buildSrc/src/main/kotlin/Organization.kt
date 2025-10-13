/**
 * Represents the distinct build flavors or organizations.
 * Using an enum ensures type safety when accessing configurations.
 *
 * @param key The string value used as a key in the configuration map.
 * @param config The AppConfig associated with the organization.
 */
sealed class Organization(val key: String, val config: AppConfig) {
    object Ooni : Organization(
        "ooni",
        AppConfig(
            appId = "org.openobservatory.ooniprobe",
            appName = "OONI Probe",
            folder = "ooniMain",
            supportsOoniRun = true,
            supportedLanguages = listOf(
                "ar",
                "ca",
                "de",
                "el",
                "es",
                "fa",
                "fr",
                "hi",
                "id",
                "is",
                "it",
                "my",
                "nl",
                "pt-rBR",
                "ro",
                "ru",
                "sk",
                "sq",
                "sw",
                "th",
                "tr",
                "vi",
                "zh-rCN",
                "zh-rTW"
            ),
        )
    )

    object Dw : Organization(
        "dw",
        AppConfig(
            appId = "com.dw.ooniprobe",
            appName = "News Media Scan",
            folder = "dwMain",
            supportsOoniRun = false,
            supportedLanguages = listOf(
                "de", "es", "fr", "pt-rBR", "ru", "tr"
            ),
        )
    )

    companion object {
        private val keyMap = listOf(Ooni, Dw).associateBy { it.key }
        fun fromKey(key: String?): Organization = keyMap[key] ?: Ooni
    }
}

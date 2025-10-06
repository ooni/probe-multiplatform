package org.ooni.probe.data.models

enum class SettingsKey(
    val value: String,
) {
    // Test Options
    AUTOMATED_TESTING_ENABLED("automated_testing_enabled"),
    AUTOMATED_TESTING_WIFIONLY("automated_testing_wifionly"),
    AUTOMATED_TESTING_CHARGING("automated_testing_charging"),
    MAX_RUNTIME_ENABLED("max_runtime_enabled"),
    MAX_RUNTIME("max_runtime"),
    DELETE_OLD_RESULTS("delete_old_results"),
    DELETE_OLD_RESULTS_THRESHOLD("delete_old_results_threshold"),

    // Website categories
    SRCH("SRCH"),
    PORN("PORN"),
    COMM("COMM"),
    COMT("COMT"),
    MMED("MMED"),
    HATE("HATE"),
    POLR("POLR"),
    PUBH("PUBH"),
    GAME("GAME"),
    PROV("PROV"),
    HACK("HACK"),
    MILX("MILX"),
    DATE("DATE"),
    ANON("ANON"),
    ALDR("ALDR"),
    GMB("GMB"),
    XED("XED"),
    REL("REL"),
    GRP("GRP"),
    GOVT("GOVT"),
    ECON("ECON"),
    LGBT("LGBT"),
    FILE("FILE"),
    HOST("HOST"),
    HUMR("HUMR"),
    NEWS("NEWS"),
    ENV("ENV"),
    CULTR("CULTR"),
    CTRL("CTRL"),
    IGO("IGO"),

    // Privacy
    UPLOAD_RESULTS("upload_results"),
    SEND_CRASH("send_crash"),

    // Proxy
    LEGACY_PROXY_HOSTNAME("proxy_hostname"),
    LEGACY_PROXY_PORT("proxy_port"),
    LEGACY_PROXY_PROTOCOL("proxy_protocol"),
    PROXIES_CUSTOM("proxies_custom"),
    PROXY_SELECTED("proxy_selected"),

    // Advanced
    LANGUAGE_SETTING("language_setting"),
    DEBUG_LOGS("debugLogs"),
    WARN_VPN_IN_USE("warn_vpn_in_use"),
    STORAGE_SIZE("storage_size"), // purely decorative

    // MISC
    DELETE_UPLOADED_JSONS("deleteUploadedJsons"),
    IS_NOTIFICATION_DIALOG("isNotificationDialog"),
    FIRST_RUN("first_run"),
    CHOSEN_WEBSITES("chosen_websites"),
    DESCRIPTOR_SECTIONS_COLLAPSED("descriptor_sections_collapsed"),

    ROUTE("route"),

    CLEAR_LEGACY_DIRECTORIES("clear_legacy_directories"),
}

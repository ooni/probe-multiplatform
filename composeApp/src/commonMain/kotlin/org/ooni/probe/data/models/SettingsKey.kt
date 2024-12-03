package org.ooni.probe.data.models

enum class SettingsKey(val value: String) {
    // Notifications
    NOTIFICATIONS_ENABLED("notifications_enabled"),

    // Test Options
    AUTOMATED_TESTING_ENABLED("automated_testing_enabled"),
    AUTOMATED_TESTING_WIFIONLY("automated_testing_wifionly"),
    AUTOMATED_TESTING_CHARGING("automated_testing_charging"),
    MAX_RUNTIME_ENABLED("max_runtime_enabled"),
    MAX_RUNTIME("max_runtime"),

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
    PROXY_HOSTNAME("proxy_hostname"),
    PROXY_PORT("proxy_port"),

    // Advanced
    THEME_ENABLED("theme_enabled"),
    LANGUAGE_SETTING("language_setting"),
    DEBUG_LOGS("debugLogs"),
    WARN_VPN_IN_USE("warn_vpn_in_use"),
    STORAGE_SIZE("storage_size"), // purely decorative

    // MISC
    DELETE_UPLOADED_JSONS("deleteUploadedJsons"),
    IS_NOTIFICATION_DIALOG("isNotificationDialog"),
    FIRST_RUN("first_run"),

    // Run Tests
    TEST_SIGNAL("test_signal"),
    RUN_HTTP_INVALID_REQUEST_LINE("run_http_invalid_request_line"),
    TEST_FACEBOOK_MESSENGER("test_facebook_messenger"),
    RUN_DASH("run_dash"),
    WEB_CONNECTIVITY("web_connectivity"),
    RUN_NDT("run_ndt"),
    TEST_PSIPHON("test_psiphon"),
    TEST_TOR("test_tor"),
    PROXY_PROTOCOL("proxy_protocol"),
    TEST_TELEGRAM("test_telegram"),
    RUN_HTTP_HEADER_FIELD_MANIPULATION("run_http_header_field_manipulation"),
    EXPERIMENTAL("experimental"),
    TEST_WHATSAPP("test_whatsapp"),

    ROUTE("route"),
}

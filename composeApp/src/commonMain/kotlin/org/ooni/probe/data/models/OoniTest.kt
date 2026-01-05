package org.ooni.probe.data.models

enum class OoniTest(
    val id: String,
    val key: String,
) {
    Websites("00104", "websites"),
    InstantMessaging("00105", "instant_messaging"),
    Circumvention("00106", "circumvention"),
    Performance("00107", "performance"),
    Experimental("00108", "experimental"),
    ;

    companion object {
        private val map = entries.associateBy(OoniTest::id)

        fun fromId(id: String) = map[id]

        fun isValidId(id: String): Boolean = fromId(id) != null
    }
}

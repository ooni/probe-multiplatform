package org.ooni.probe.data.models

sealed class OoniTest(
    val id: String,
    val key: String,
) {
    object Websites : OoniTest("00104", "websites")

    object InstantMessaging : OoniTest("00105", "instant_messaging")

    object Circumvention : OoniTest("00106", "circumvention")

    object Performance : OoniTest("00107", "performance")

    object Experimental : OoniTest("00108", "experimental")

    companion object {
        fun fromId(id: String): OoniTest? =
            when (id) {
                Websites.id -> Websites
                InstantMessaging.id -> InstantMessaging
                Circumvention.id -> Circumvention
                Performance.id -> Performance
                Experimental.id -> Experimental
                else -> null
            }

        fun isValidId(id: String): Boolean = fromId(id) != null
    }
}

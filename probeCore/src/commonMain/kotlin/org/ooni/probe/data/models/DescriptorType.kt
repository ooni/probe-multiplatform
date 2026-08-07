package org.ooni.probe.data.models

enum class DescriptorType(
    val key: String,
    val titleKey: String,
) {
    Default("default", "Dashboard_RunV2_Ooni_Title"),
    Installed("installed", "Dashboard_RunV2_Title"),
    ;

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}

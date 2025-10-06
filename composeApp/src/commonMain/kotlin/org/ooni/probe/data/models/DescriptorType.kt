package org.ooni.probe.data.models

import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_Ooni_Title
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_Title
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.StringResource

enum class DescriptorType(
    val key: String,
    val title: StringResource,
) {
    Default("default", Res.string.Dashboard_RunV2_Ooni_Title),
    Installed("installed", Res.string.Dashboard_RunV2_Title),
    ;

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}

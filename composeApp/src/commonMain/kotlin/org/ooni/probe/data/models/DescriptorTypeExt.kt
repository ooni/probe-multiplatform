package org.ooni.probe.data.models

import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_Ooni_Title
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_Title
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.StringResource

val DescriptorType.titleRes: StringResource
    get() = when (this) {
        DescriptorType.Default -> Res.string.Dashboard_RunV2_Ooni_Title
        DescriptorType.Installed -> Res.string.Dashboard_RunV2_Title
    }

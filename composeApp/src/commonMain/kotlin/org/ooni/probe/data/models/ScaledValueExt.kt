package org.ooni.probe.data.models

import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_Gbps
import ooniprobe.composeapp.generated.resources.TestResults_Kbps
import ooniprobe.composeapp.generated.resources.TestResults_Mbps
import org.jetbrains.compose.resources.StringResource

val ScaledValue.unitStringRes: StringResource
    get() = when (unitStringKey) {
        "TestResults_Kbps" -> Res.string.TestResults_Kbps
        "TestResults_Mbps" -> Res.string.TestResults_Mbps
        "TestResults_Gbps" -> Res.string.TestResults_Gbps
        else -> Res.string.TestResults_Kbps
    }

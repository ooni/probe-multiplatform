package org.ooni.probe.data.models

import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_Gbps
import ooniprobe.composeapp.generated.resources.TestResults_Kbps
import ooniprobe.composeapp.generated.resources.TestResults_Mbps
import org.jetbrains.compose.resources.StringResource

val ScaledValue.unitStringRes: StringResource
    get() = when (unit) {
        ScaledValue.Unit.KB -> Res.string.TestResults_Kbps
        ScaledValue.Unit.MB -> Res.string.TestResults_Mbps
        ScaledValue.Unit.GB -> Res.string.TestResults_Gbps
    }

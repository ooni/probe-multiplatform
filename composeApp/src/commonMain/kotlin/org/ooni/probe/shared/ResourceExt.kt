package org.ooni.probe.shared

import androidx.compose.runtime.Composable
import ooniprobe.composeapp.generated.resources.Common_Months_April
import ooniprobe.composeapp.generated.resources.Common_Months_August
import ooniprobe.composeapp.generated.resources.Common_Months_December
import ooniprobe.composeapp.generated.resources.Common_Months_February
import ooniprobe.composeapp.generated.resources.Common_Months_January
import ooniprobe.composeapp.generated.resources.Common_Months_July
import ooniprobe.composeapp.generated.resources.Common_Months_June
import ooniprobe.composeapp.generated.resources.Common_Months_March
import ooniprobe.composeapp.generated.resources.Common_Months_May
import ooniprobe.composeapp.generated.resources.Common_Months_November
import ooniprobe.composeapp.generated.resources.Common_Months_October
import ooniprobe.composeapp.generated.resources.Common_Months_September
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun stringMonthArrayResource(): List<String> =
    listOf(
        Res.string.Common_Months_January,
        Res.string.Common_Months_February,
        Res.string.Common_Months_March,
        Res.string.Common_Months_April,
        Res.string.Common_Months_May,
        Res.string.Common_Months_June,
        Res.string.Common_Months_July,
        Res.string.Common_Months_August,
        Res.string.Common_Months_September,
        Res.string.Common_Months_October,
        Res.string.Common_Months_November,
        Res.string.Common_Months_December,
    ).map { stringResource(it) }

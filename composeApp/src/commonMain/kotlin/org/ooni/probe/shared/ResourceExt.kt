package org.ooni.probe.shared

import androidx.compose.runtime.Composable
import ooniprobe.composeapp.generated.resources.Common_Hour_One
import ooniprobe.composeapp.generated.resources.Common_Hour_Other
import ooniprobe.composeapp.generated.resources.Common_Minutes_One
import ooniprobe.composeapp.generated.resources.Common_Minutes_Other
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
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_RunButton_Label_One
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_RunButton_Label_Other
import ooniprobe.composeapp.generated.resources.Measurements_Count_One
import ooniprobe.composeapp.generated.resources.Measurements_Count_Other
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

val stringMap = mapOf(
    "@string/Common_Minutes_One" to Res.string.Common_Minutes_One,
    "@string/Common_Minutes_Other" to Res.string.Common_Minutes_Other,
    "@string/Common_Hour_One" to Res.string.Common_Hour_One,
    "@string/Common_Hour_Other" to Res.string.Common_Hour_Other,
    "@string/Dashboard_RunTests_RunButton_Label_One" to Res.string.Dashboard_RunTests_RunButton_Label_One,
    "@string/Dashboard_RunTests_RunButton_Label_Other" to Res.string.Dashboard_RunTests_RunButton_Label_Other,
    "@string/Measurements_Count_One" to Res.string.Measurements_Count_One,
    "@string/Measurements_Count_Other" to Res.string.Measurements_Count_Other,
)

@Composable
fun stringMonthArrayResource(): List<String> {
    return listOf(
        stringResource(Res.string.Common_Months_January),
        stringResource(Res.string.Common_Months_February),
        stringResource(Res.string.Common_Months_March),
        stringResource(Res.string.Common_Months_April),
        stringResource(Res.string.Common_Months_May),
        stringResource(Res.string.Common_Months_June),
        stringResource(Res.string.Common_Months_July),
        stringResource(Res.string.Common_Months_August),
        stringResource(Res.string.Common_Months_September),
        stringResource(Res.string.Common_Months_October),
        stringResource(Res.string.Common_Months_November),
        stringResource(Res.string.Common_Months_December),
    )
}

@Composable
fun pluralStringResourceItem(
    resource: PluralStringResource,
    quantity: Int,
    vararg formatArgs: Any,
): String {
    return stringMap[pluralStringResource(resource, quantity, formatArgs)]?.let {
        return stringResource(it, *formatArgs)
    } ?: ""
}

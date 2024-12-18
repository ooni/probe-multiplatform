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
import ooniprobe.composeapp.generated.resources.Measurements_Failed_One
import ooniprobe.composeapp.generated.resources.Measurements_Failed_Other
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Circumvention_Available_Plural
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Circumvention_Available_Singular
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Circumvention_Blocked_Plural
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Circumvention_Blocked_Singular
import ooniprobe.composeapp.generated.resources.TestResults_Overview_InstantMessaging_Available_Plural
import ooniprobe.composeapp.generated.resources.TestResults_Overview_InstantMessaging_Available_Singular
import ooniprobe.composeapp.generated.resources.TestResults_Overview_InstantMessaging_Blocked_Plural
import ooniprobe.composeapp.generated.resources.TestResults_Overview_InstantMessaging_Blocked_Singular
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Websites_Blocked_Plural
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Websites_Blocked_Singular
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Websites_Tested_Plural
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Websites_Tested_Singular
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
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

suspend fun getPluralStringResourceItem(
    resource: PluralStringResource,
    quantity: Int,
    vararg formatArgs: Any,
): String {
    return stringMap[getPluralString(resource, quantity, formatArgs)]?.let {
        return getString(it, *formatArgs)
    } ?: ""
}

private val stringMap = mapOf(
    "@string/Common_Minutes_One"
        to Res.string.Common_Minutes_One,
    "@string/Common_Minutes_Other"
        to Res.string.Common_Minutes_Other,
    "@string/Common_Hour_One"
        to Res.string.Common_Hour_One,
    "@string/Common_Hour_Other"
        to Res.string.Common_Hour_Other,
    "@string/Dashboard_RunTests_RunButton_Label_One"
        to Res.string.Dashboard_RunTests_RunButton_Label_One,
    "@string/Dashboard_RunTests_RunButton_Label_Other"
        to Res.string.Dashboard_RunTests_RunButton_Label_Other,
    "@string/Measurements_Count_One"
        to Res.string.Measurements_Count_One,
    "@string/Measurements_Count_Other"
        to Res.string.Measurements_Count_Other,
    "@string/Measurements_Failed_One"
        to Res.string.Measurements_Failed_One,
    "@string/Measurements_Failed_Other"
        to Res.string.Measurements_Failed_Other,
    "@string/TestResults_Overview_Websites_Blocked_Singular"
        to Res.string.TestResults_Overview_Websites_Blocked_Singular,
    "@string/TestResults_Overview_Websites_Blocked_Plural"
        to Res.string.TestResults_Overview_Websites_Blocked_Plural,
    "@string/TestResults_Overview_Websites_Tested_Singular"
        to Res.string.TestResults_Overview_Websites_Tested_Singular,
    "@string/TestResults_Overview_Websites_Tested_Plural"
        to Res.string.TestResults_Overview_Websites_Tested_Plural,
    "@string/TestResults_Overview_InstantMessaging_Blocked_Singular"
        to Res.string.TestResults_Overview_InstantMessaging_Blocked_Singular,
    "@string/TestResults_Overview_InstantMessaging_Blocked_Plural"
        to Res.string.TestResults_Overview_InstantMessaging_Blocked_Plural,
    "@string/TestResults_Overview_InstantMessaging_Available_Singular"
        to Res.string.TestResults_Overview_InstantMessaging_Available_Singular,
    "@string/TestResults_Overview_InstantMessaging_Available_Plural"
        to Res.string.TestResults_Overview_InstantMessaging_Available_Plural,
    "@string/TestResults_Overview_Circumvention_Blocked_Singular"
        to Res.string.TestResults_Overview_Circumvention_Blocked_Singular,
    "@string/TestResults_Overview_Circumvention_Blocked_Plural"
        to Res.string.TestResults_Overview_Circumvention_Blocked_Plural,
    "@string/TestResults_Overview_Circumvention_Available_Singular"
        to Res.string.TestResults_Overview_Circumvention_Available_Singular,
    "@string/TestResults_Overview_Circumvention_Available_Plural"
        to Res.string.TestResults_Overview_Circumvention_Available_Plural,
)

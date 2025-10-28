package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import ooniprobe.composeapp.generated.resources.Common_Ago
import ooniprobe.composeapp.generated.resources.Common_Hours
import ooniprobe.composeapp.generated.resources.Common_Hours_Abbreviated
import ooniprobe.composeapp.generated.resources.Common_Minutes
import ooniprobe.composeapp.generated.resources.Common_Minutes_Abbreviated
import ooniprobe.composeapp.generated.resources.Common_Seconds
import ooniprobe.composeapp.generated.resources.Common_Seconds_Abbreviated
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.shared.stringMonthArrayResource
import org.ooni.probe.shared.today
import kotlin.time.Clock
import kotlin.time.Duration

private val longDateTimeFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour()
    char(':')
    minute()
}

private val logDateTimeFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

@Composable
fun LocalDateTime.relativeDateTime(): String =
    if (date == LocalDate.today()) {
        val diff = (Clock.System.now() - toInstant(TimeZone.currentSystemDefault()))
        val diffString = diff.toComponents { hours, minutes, _, _ ->
            if (hours > 0) {
                pluralStringResource(Res.plurals.Common_Hours, hours.toInt(), hours.toInt())
            } else {
                pluralStringResource(Res.plurals.Common_Minutes, minutes, minutes)
            }
        }
        stringResource(Res.string.Common_Ago, diffString)
    } else {
        longFormat()
    }

fun LocalDateTime.longFormat(): String = format(longDateTimeFormat)

fun LocalDateTime.logFormat(): String = format(logDateTimeFormat)

@Composable
fun LocalDateTime.articleFormat(): String {
    val monthNames = stringMonthArrayResource()
    return LocalDateTime
        .Format {
            day(Padding.NONE)
            char(' ')
            monthName(MonthNames(monthNames))
            char(' ')
            year()
        }.format(this)
}

@Composable
fun Duration.format(abbreviated: Boolean = true): String =
    toComponents { hours, minutes, seconds, _ ->
        listOfNotNull(
            if (hours > 0) {
                val value = hours.toInt()
                if (abbreviated) {
                    stringResource(Res.string.Common_Hours_Abbreviated, value)
                } else {
                    pluralStringResource(Res.plurals.Common_Hours, value, value)
                }
            } else {
                null
            },
            if (minutes > 0) {
                if (abbreviated) {
                    stringResource(Res.string.Common_Minutes_Abbreviated, minutes)
                } else {
                    pluralStringResource(Res.plurals.Common_Minutes, minutes, minutes)
                }
            } else {
                null
            },
            if (seconds > 0) {
                if (abbreviated) {
                    stringResource(Res.string.Common_Seconds_Abbreviated, seconds)
                } else {
                    pluralStringResource(Res.plurals.Common_Seconds, seconds, seconds)
                }
            } else {
                null
            },
        ).joinToString(" ")
    }

fun LocalDate.isoFormat() = format(LocalDate.Formats.ISO)

package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import ooniprobe.composeapp.generated.resources.Common_Ago
import ooniprobe.composeapp.generated.resources.Common_Hours
import ooniprobe.composeapp.generated.resources.Common_Hours_Abbreviated
import ooniprobe.composeapp.generated.resources.Common_Minutes
import ooniprobe.composeapp.generated.resources.Common_Minutes_Abbreviated
import ooniprobe.composeapp.generated.resources.Common_Seconds_Abbreviated
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.shared.pluralStringResourceItem
import org.ooni.probe.shared.today
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
                pluralStringResourceItem(Res.plurals.Common_Hours, hours.toInt(), hours.toInt())
            } else {
                pluralStringResourceItem(Res.plurals.Common_Minutes, minutes, minutes)
            }
        }
        stringResource(Res.string.Common_Ago, diffString)
    } else {
        longFormat()
    }

fun LocalDateTime.longFormat(): String = format(longDateTimeFormat)

fun LocalDateTime.logFormat(): String = format(logDateTimeFormat)

@Composable
fun Duration.shortFormat(): String =
    toComponents { hours, minutes, seconds, _ ->
        (
            if (hours > 0) {
                stringResource(Res.string.Common_Hours_Abbreviated, hours.toInt())
            } else {
                ""
            }
        ) + " " + (
            if (minutes > 0) {
                stringResource(Res.string.Common_Minutes_Abbreviated, minutes)
            } else {
                ""
            }
        ) + " " + (
            if (seconds > 0) {
                stringResource(Res.string.Common_Seconds_Abbreviated, seconds)
            } else {
                ""
            }
        )
    }.trimStart()

package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ago
import ooniprobe.composeapp.generated.resources.hours
import ooniprobe.composeapp.generated.resources.hours_abbreviated
import ooniprobe.composeapp.generated.resources.minutes
import ooniprobe.composeapp.generated.resources.minutes_abbreviated
import ooniprobe.composeapp.generated.resources.seconds_abbreviated
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
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
                pluralStringResource(Res.plurals.hours, hours.toInt(), hours.toInt())
            } else {
                pluralStringResource(Res.plurals.minutes, minutes, minutes)
            }
        }
        stringResource(Res.string.ago, diffString)
    } else {
        longFormat()
    }

fun LocalDateTime.longFormat(): String = format(longDateTimeFormat)

fun LocalDateTime.logFormat(): String = format(logDateTimeFormat)

@Composable
fun Duration.shortFormat(): String =
    toComponents { hours, minutes, seconds, _ ->
        (if (hours > 0) stringResource(Res.string.hours_abbreviated, hours.toInt()) else "") +
            " " +
            (if (minutes > 0) stringResource(Res.string.minutes_abbreviated, minutes) else "") +
            " " +
            (if (seconds > 0) stringResource(Res.string.seconds_abbreviated, seconds) else "")
    }.trimStart()

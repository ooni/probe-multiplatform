package org.ooni.probe.shared

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

fun LocalDateTime.toEpoch() = toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

fun LocalDate.toEpoch() = atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

fun LocalDate.toEpochInUTC() = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

fun Long.toLocalDateTime() = Instant.fromEpochMilliseconds(this).toLocalDateTime()

fun Long.toLocalDateFromUtc() = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

fun LocalDate.Companion.today() = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun LocalDateTime.Companion.now() = Clock.System.now().toLocalDateTime()

fun Instant.toLocalDateTime() = toLocalDateTime(TimeZone.currentSystemDefault())

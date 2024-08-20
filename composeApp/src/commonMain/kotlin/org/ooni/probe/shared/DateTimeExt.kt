package org.ooni.probe.shared

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

fun LocalDateTime.toEpoch() = toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

fun Long.toLocalDateTime() = Instant.fromEpochMilliseconds(this).toLocalDateTime()

fun LocalDate.Companion.today() = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun LocalDateTime.Companion.now() = Clock.System.now().toLocalDateTime()

fun Instant.toLocalDateTime() = toLocalDateTime(TimeZone.currentSystemDefault())

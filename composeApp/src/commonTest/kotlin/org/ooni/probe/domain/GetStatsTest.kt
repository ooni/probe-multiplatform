package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.ooni.probe.data.models.MeasurementStats
import org.ooni.probe.shared.toDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class GetStatsTest {
    @Test
    fun queriesCountsFromStartOfTodayWeekMonthAndEpoch() =
        runTest {
            val requestedStartTimes = mutableListOf<LocalDateTime>()
            val subject = buildSubject(
                currentDate = flowOf(WEDNESDAY),
                countMeasurementsFromStartTime = {
                    requestedStartTimes += it
                    flowOf(0L)
                },
            )

            subject().first()

            assertEquals(
                listOf(WEDNESDAY, MONDAY, FIRST_OF_NOVEMBER, EPOCH).map { it.toDateTime() },
                requestedStartTimes,
            )
        }

    @Test
    fun mapsCountsNetworksAndCountries() =
        runTest {
            val counts = mapOf(
                WEDNESDAY to 1L,
                MONDAY to 2L,
                FIRST_OF_NOVEMBER to 3L,
                EPOCH to 4L,
            )
            val subject = buildSubject(
                currentDate = flowOf(WEDNESDAY),
                countMeasurementsFromStartTime = { flowOf(counts.getValue(it.date)) },
                countNetworkAsns = { flowOf(5L) },
                getNetworkCountries = { flowOf(listOf("PT", "IT", "BR")) },
            )

            val stats = subject().first()

            assertEquals(
                MeasurementStats(
                    measurementsToday = 1L,
                    measurementsWeek = 2L,
                    measurementsMonth = 3L,
                    measurementsTotal = 4L,
                    networks = 5L,
                    countries = listOf("Brazil", "Italy", "Portugal"),
                ),
                stats,
            )
        }

    @Test
    fun emitsAgainWhenUpstreamChanges() =
        runTest {
            val total = MutableStateFlow(10L)
            val networks = MutableStateFlow(1L)
            val countries = MutableStateFlow(listOf("PT"))
            val subject = buildSubject(
                currentDate = flowOf(WEDNESDAY),
                countMeasurementsFromStartTime = { if (it.date == EPOCH) total else flowOf(0L) },
                countNetworkAsns = { networks },
                getNetworkCountries = { countries },
            )
            val emissions = mutableListOf<MeasurementStats>()
            subject().onEach { emissions += it }.launchIn(backgroundScope)
            runCurrent()

            total.value = 11L
            runCurrent()
            networks.value = 2L
            runCurrent()
            countries.value = listOf("PT", "IT")
            runCurrent()

            val last = emissions.last()
            assertEquals(4, emissions.size)
            assertEquals(11L, last.measurementsTotal)
            assertEquals(2L, last.networks)
            assertEquals(listOf("Italy", "Portugal"), last.countries)
        }

    @Test
    fun recomputesStartTimesWhenDateChangesAtMidnight() =
        runTest {
            // Monday 30 Nov -> Tuesday 1 Dec: today and month move, week stays the same
            val monday = LocalDate(2026, 11, 30)
            val tuesday = LocalDate(2026, 12, 1)
            val currentDate = MutableStateFlow(monday)
            // Count = day of month of the start time, so we can tell which query answered
            val subject = buildSubject(
                currentDate = currentDate,
                countMeasurementsFromStartTime = { flowOf(it.date.day.toLong()) },
            )
            val emissions = mutableListOf<MeasurementStats>()
            subject().onEach { emissions += it }.launchIn(backgroundScope)
            runCurrent()

            assertEquals(30L, emissions.last().measurementsToday)
            assertEquals(30L, emissions.last().measurementsWeek)
            assertEquals(1L, emissions.last().measurementsMonth)

            currentDate.value = tuesday
            runCurrent()

            assertEquals(2, emissions.size)
            val afterMidnight = emissions.last()
            assertEquals(1L, afterMidnight.measurementsToday)
            assertEquals(30L, afterMidnight.measurementsWeek)
            assertEquals(1L, afterMidnight.measurementsMonth)
            assertEquals(1L, afterMidnight.measurementsTotal)
        }

    @Test
    fun cancelsQueriesForPreviousDate() =
        runTest {
            val currentDate = MutableStateFlow(LocalDate(2026, 11, 29)) // Sunday
            val activeQueries = mutableListOf<LocalDate>()
            val subject = buildSubject(
                currentDate = currentDate,
                countMeasurementsFromStartTime = { start ->
                    MutableStateFlow(0L)
                        .onStart { activeQueries += start.date }
                        .onCompletion { activeQueries -= start.date }
                },
            )
            subject().launchIn(backgroundScope)
            runCurrent()
            assertEquals(
                listOf(LocalDate(2026, 11, 29), LocalDate(2026, 11, 23), FIRST_OF_NOVEMBER, EPOCH),
                activeQueries,
            )

            currentDate.value = LocalDate(2026, 11, 30) // Monday
            runCurrent()

            assertEquals(
                listOf(LocalDate(2026, 11, 30), LocalDate(2026, 11, 30), FIRST_OF_NOVEMBER, EPOCH),
                activeQueries,
            )
        }

    private fun buildSubject(
        currentDate: Flow<LocalDate>,
        countMeasurementsFromStartTime: (LocalDateTime) -> Flow<Long> = { flowOf(0L) },
        countNetworkAsns: () -> Flow<Long> = { flowOf(0L) },
        getNetworkCountries: () -> Flow<List<String>> = { flowOf(emptyList()) },
    ) = GetStats(
        currentDate = { currentDate },
        countMeasurementsFromStartTime = countMeasurementsFromStartTime,
        countNetworkAsns = countNetworkAsns,
        getNetworkCountries = getNetworkCountries,
        getCountryNameByCode = { COUNTRY_NAMES.getValue(it) },
    )

    companion object {
        private val WEDNESDAY = LocalDate(2026, 11, 11)
        private val MONDAY = LocalDate(2026, 11, 9)
        private val FIRST_OF_NOVEMBER = LocalDate(2026, 11, 1)
        private val EPOCH = LocalDate.fromEpochDays(0)
        private val COUNTRY_NAMES = mapOf("PT" to "Portugal", "IT" to "Italy", "BR" to "Brazil")
    }
}

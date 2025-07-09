package org.ooni.engine

import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.TaskEvent
import org.ooni.engine.models.TaskEventResult
import org.ooni.probe.di.Dependencies
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskEventMapperTest {
    private val json = Dependencies.buildJson()
    private val subject =
        TaskEventMapper(
            json = json,
            networkTypeFinder = { NetworkType.NoInternet },
        )

    @Test
    fun started() {
        val result =
            json.decodeFromString<TaskEventResult>(
                """{"key":"status.started","value":{}}""",
            )

        val event = subject(result)!!

        assertEquals(TaskEvent.Started::class, event::class)
    }

    @Test
    fun log() {
        val result =
            json.decodeFromString<TaskEventResult>(
                """{"key":"log","value":{"log_level":"INFO","message":"Looking up OONI backends... please, be patient"}}""",
            )

        val event = subject(result)!!

        assertEquals(TaskEvent.Log::class, event::class)
        with(event as TaskEvent.Log) {
            assertEquals("INFO", level)
            assertEquals("Looking up OONI backends... please, be patient", message)
        }
    }

    @Test
    fun progress() {
        val result =
            json.decodeFromString<TaskEventResult>(
                """{"key":"status.progress","value":{"message":"contacted bouncer","percentage":0.1}}""",
            )

        val event = subject(result)!!

        assertEquals(TaskEvent.Progress::class, event::class)
        with(event as TaskEvent.Progress) {
            assertEquals(0.1, progress, 0.01)
            assertEquals("contacted bouncer", message)
        }
    }

    @Test
    fun geoIpLookup() {
        val result =
            json.decodeFromString<TaskEventResult>(
                @Suppress("ktlint:standard:max-line-length")
                """{"key":"status.geoip_lookup","value":{"probe_asn":"AS12345","probe_cc":"PT","probe_ip":"1.2.3.4","probe_network_name":"Vodafone"}}""",
            )

        val event = subject(result)!!

        assertEquals(TaskEvent.GeoIpLookup::class, event::class)
        with(event as TaskEvent.GeoIpLookup) {
            assertEquals("AS12345", asn)
            assertEquals("PT", countryCode)
            assertEquals("1.2.3.4", ip)
            assertEquals("Vodafone", networkName)
            assertEquals(NetworkType.NoInternet, networkType)
        }
    }

    @Test
    fun measurementStart() {
        val result =
            json.decodeFromString<TaskEventResult>(
                """{"key":"status.measurement_start","value":{"idx":0,"input":"https://www.reddit.com/"}}""",
            )

        val event = subject(result)!!

        assertEquals(TaskEvent.MeasurementStart::class, event::class)
        with(event as TaskEvent.MeasurementStart) {
            assertEquals(0, index)
            assertEquals("https://www.reddit.com/", url)
        }
    }

    @Test
    fun measurement() {
        val result =
            json.decodeFromString<TaskEventResult>(
                @Suppress("ktlint:standard:max-line-length")
                """{"key":"measurement","value":{"idx":99,"input":"https://www.reddit.com/","json_str":"{\"annotations\":{\"architecture\":\"arm64\",\"engine_name\":\"ooniprobe-engine\",\"engine_version\":\"3.22.0\",\"flavor\":\"ooniprobe\",\"go_version\":\"go1.21.10\",\"network_type\":\"wifi\",\"origin\":\"ooni-run\",\"platform\":\"android\",\"vcs_modified\":\"\",\"vcs_revision\":\"\",\"vcs_time\":\"\",\"vcs_tool\":\"\"},\"data_format_version\":\"0.2.0\",\"input\":\"https://www.reddit.com/\",\"measurement_start_time\":\"2024-08-05 13:22:31\",\"probe_asn\":\"AS12345\",\"probe_cc\":\"PT\",\"probe_ip\":\"127.0.0.1\",\"probe_network_name\":\"Vodafone\",\"report_id\":\"\",\"resolver_asn\":\"AS12345\",\"resolver_ip\":\"1.2.3.4\",\"resolver_network_name\":\"Vodafone\",\"software_name\":\"ooniprobe\",\"software_version\":\"1.0\"}"}}""",
            )

        val event = subject(result)!!

        assertEquals(TaskEvent.Measurement::class, event::class)
        with(event as TaskEvent.Measurement) {
            assertEquals(99, index)
            with(event.result!!) {
                assertEquals("127.0.0.1", probeIp)
                assertEquals("AS12345", probeAsn)
                assertEquals("PT", probeCountryCode)
                assertEquals("", reportId)
            }
            with(event.result?.measurementStartTime?.toLocalDateTime(TimeZone.UTC)!!) {
                // 2024-08-05 13:22:31
                assertEquals(2024, year)
                assertEquals(Month.AUGUST, month)
                assertEquals(5, day)
                assertEquals(13, hour)
                assertEquals(22, minute)
                assertEquals(31, second)
            }
            assertEquals("https://www.reddit.com/", event.result?.input)
        }
    }

    @Test
    fun measurementDone() {
        val result =
            json.decodeFromString<TaskEventResult>(
                """{"key":"status.measurement_done","value":{"idx":3,"input":"https://www.reddit.com/"}}""",
            )

        val event = subject(result)!!

        assertEquals(TaskEvent.MeasurementDone::class, event::class)
        with(event as TaskEvent.MeasurementDone) {
            assertEquals(3, index)
        }
    }

    @Test
    fun end() {
        val result =
            json.decodeFromString<TaskEventResult>(
                """{"key":"status.end","value":{"downloaded_kb":692.8134765625,"failure":"","uploaded_kb":4.994140625}}""",
            )

        val event = subject(result)!!

        assertEquals(TaskEvent.End::class, event::class)
    }

    @Test
    fun taskTerminated() {
        val result =
            json.decodeFromString<TaskEventResult>(
                """{"key":"task_terminated","value":{}}""",
            )

        val event = subject(result)!!

        assertEquals(TaskEvent.TaskTerminated::class, event::class)
    }
}

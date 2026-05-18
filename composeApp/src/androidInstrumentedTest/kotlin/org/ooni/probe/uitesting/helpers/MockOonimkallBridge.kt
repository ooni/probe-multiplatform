package org.ooni.probe.uitesting.helpers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.ooni.engine.OonimkallBridge

/**
 * A fully offline, deterministic [OonimkallBridge] for instrumented tests.
 *
 * Replaces the real `AndroidOonimkallBridge` so tests never hit the OONI
 * backend. [startTask] is test-aware: it reads the `name` (test_name) out of
 * the serialized [org.ooni.engine.models.TaskSettings] and replays a canned
 * event sequence that drives [org.ooni.probe.domain.RunNetTest] to completion
 * instantly, producing a stored, uploaded measurement.
 *
 * The generated `measurement_uid` embeds the test slug (test name with
 * underscores removed) so the measurement-detail Explorer URL contains the
 * test name, satisfying `checkUrlInsideWebView(...)` without any network.
 *
 * `checkIn`, `httpDo` and `submitMeasurement` are backed by overridable
 * lambdas with safe, network-free defaults.
 */
class MockOonimkallBridge : OonimkallBridge {
    var checkIn: (OonimkallBridge.CheckInConfig) -> OonimkallBridge.CheckInResults = {
        OonimkallBridge.CheckInResults(reportId = null, urls = emptyList())
    }

    var httpDo: (OonimkallBridge.HTTPRequest) -> OonimkallBridge.HTTPResponse = {
        // Inert default; tests that exercise descriptor fetch override this
        // via setupMockedEngine { httpDo = ... }.
        OonimkallBridge.HTTPResponse(body = "{}")
    }

    var submitMeasurement: (String) -> OonimkallBridge.SubmitMeasurementResults = {
        OonimkallBridge.SubmitMeasurementResults(
            updatedMeasurement = null,
            updatedReportId = REPORT_ID,
            measurementUid = "mock-uid",
        )
    }

    var lastStartTaskSettingsSerialized: String? = null
        private set

    override fun startTask(settingsSerialized: String): OonimkallBridge.Task {
        lastStartTaskSettingsSerialized = settingsSerialized
        val testName = runCatching {
            (json.parseToJsonElement(settingsSerialized) as JsonObject)["name"]
                ?.toString()?.trim('"')
        }.getOrNull() ?: "web_connectivity"
        val slug = testName.replace("_", "")
        val measurementUid = "$slug-mock-uid"

        val measurementJson = buildJsonObject {
            put("report_id", REPORT_ID)
            put("test_name", testName)
            put("test_keys", buildJsonObject {})
        }.toString()

        val events = ArrayDeque(
            listOf(
                event("status.started") {},
                event("status.geoip_lookup") {
                    put("probe_ip", "1.2.3.4")
                    put("probe_asn", "AS12345")
                    put("probe_cc", "US")
                    put("probe_network_name", "Example ISP")
                    put("geoip_db", "")
                },
                event("status.report_create") { put("report_id", REPORT_ID) },
                event("status.measurement_start") {
                    put("idx", 0)
                    put("input", "")
                },
                event("measurement") {
                    put("idx", 0)
                    put("json_str", measurementJson)
                },
                event("status.measurement_submission") {
                    put("idx", 0)
                    put("measurement_uid", measurementUid)
                },
                event("status.measurement_done") { put("idx", 0) },
                event("status.end") {
                    put("downloaded_kb", 1.0)
                    put("uploaded_kb", 1.0)
                },
            ),
        )

        return object : OonimkallBridge.Task {
            override fun interrupt() = events.clear()

            override fun isDone() = events.isEmpty()

            override fun waitForNextEvent(): String =
                if (events.isEmpty()) {
                    event("task_terminated") { put("idx", 0) }
                } else {
                    events.removeFirst()
                }
        }
    }

    override fun newSession(sessionConfig: OonimkallBridge.SessionConfig): OonimkallBridge.Session = Session()

    inner class Session : OonimkallBridge.Session {
        override fun submitMeasurement(measurement: String) = this@MockOonimkallBridge.submitMeasurement(measurement)

        override fun checkIn(config: OonimkallBridge.CheckInConfig) = this@MockOonimkallBridge.checkIn(config)

        override fun httpDo(request: OonimkallBridge.HTTPRequest) = this@MockOonimkallBridge.httpDo(request)

        override fun close() = Unit
    }

    private fun event(
        key: String,
        value: JsonObjectBuilder.() -> Unit,
    ): String =
        buildJsonObject {
            put("key", key)
            put("value", buildJsonObject(value))
        }.toString()

    companion object {
        private const val REPORT_ID = "20240101T000000Z_mock_US_AS12345_n1_mockreportid"
        private val json = Json { ignoreUnknownKeys = true }
    }
}

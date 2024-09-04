package org.ooni.engine

interface OonimkallBridge {
    @Throws(Exception::class)
    fun startTask(settingsSerialized: String): Task

    interface Task {
        fun interrupt()

        fun isDone(): Boolean

        fun waitForNextEvent(): String
    }

    @Throws(Exception::class)
    fun newSession(sessionConfig: SessionConfig): Session

    interface Logger {
        fun debug(msg: String?)

        fun info(msg: String?)

        fun warn(msg: String?)
    }

    data class SessionConfig(
        val softwareName: String,
        val softwareVersion: String,
        val proxy: String?,
        val probeServicesURL: String?,
        val assetsDir: String,
        val stateDir: String,
        val tempDir: String,
        val tunnelDir: String,
        val logger: Logger?,
        val verbose: Boolean,
    )

    interface Session {
        @Throws(Exception::class)
        fun submitMeasurement(measurement: String): SubmitMeasurementResults

        @Throws(Exception::class)
        fun checkIn(config: CheckInConfig): CheckInResults

        @Throws(Exception::class)
        fun httpDo(request: HTTPRequest): HTTPResponse
    }

    data class SubmitMeasurementResults(
        val updatedMeasurement: String?,
        val updatedReportId: String,
    )

    data class CheckInConfig(
        val charging: Boolean,
        val onWiFi: Boolean,
        // "android" or "ios"
        val platform: String,
        // "timed"
        val runType: String,
        val softwareName: String,
        val softwareVersion: String,
        val webConnectivityCategories: List<String>,
    )

    data class CheckInResults(
        val reportId: String?,
        val urls: List<UrlInfo>,
    )

    data class UrlInfo(
        val url: String,
        val categoryCode: String?,
        val countryCode: String?,
    )

    data class HTTPRequest(
        val method: String,
        val url: String,
    )

    data class HTTPResponse(
        val body: String?,
    )
}

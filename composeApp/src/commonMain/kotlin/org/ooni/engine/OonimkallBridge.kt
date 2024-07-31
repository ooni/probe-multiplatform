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

    interface SessionConfig {
        val softwareName: String
        val softwareVersion: String

        val proxy: String?
        val probeServicesURL: String?

        val assetsDir: String
        val stateDir: String
        val tempDir: String
        val tunnelDir: String

        val logger: Logger?
        val verbose: Boolean
    }

    interface Session {
        @Throws(Exception::class)
        fun submitMeasurement(measurement: String): SubmitMeasurementResults

        @Throws(Exception::class)
        fun checkIn(config: CheckInConfig): CheckInResults

        @Throws(Exception::class)
        fun httpDo(request: HTTPRequest): HTTPResponse
    }

    interface SubmitMeasurementResults {
        val updatedMeasurement: String?
        val updatedReportId: String?
    }

    interface CheckInConfig {
        val charging: Boolean
        val onWiFi: Boolean
        val platform: String // "android" or "ios"
        val runType: String // "timed"
        val softwareName: String
        val softwareVersion: String
        val webConnectivityCategories: List<String>
    }

    interface CheckInResults {
        val reportId: String?
        val urls: List<UrlInfo>
    }

    interface UrlInfo {
        val url: String
        val categoryCode: String?
        val countryCode: String?
    }

    interface HTTPRequest {
        val method: String
        val url: String
    }

    interface HTTPResponse {
        val body: String?
    }
}

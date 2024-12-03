package org.ooni.engine

/*
 Bridge implementation to be used in tests
 */
class TestOonimkallBridge : OonimkallBridge {
    // Test helpers

    private val nextEvents = mutableListOf<String>()

    fun addNextEvents(vararg events: String) {
        nextEvents.addAll(events)
    }

    var lastStartTaskSettingsSerialized: String? = null
        private set

    var lastSessionConfig: OonimkallBridge.SessionConfig? = null
        private set

    var submitMeasurementMock: ((String) -> OonimkallBridge.SubmitMeasurementResults)? = null
    var checkInMock: ((OonimkallBridge.CheckInConfig) -> OonimkallBridge.CheckInResults)? = null
    var httpDoMock: ((OonimkallBridge.HTTPRequest) -> OonimkallBridge.HTTPResponse)? = null

    // Base implementation

    override fun startTask(settingsSerialized: String): OonimkallBridge.Task {
        lastStartTaskSettingsSerialized = settingsSerialized
        return object : OonimkallBridge.Task {
            override fun interrupt() {}

            override fun isDone() = nextEvents.isEmpty()

            override fun waitForNextEvent(): String = nextEvents.removeAt(0)
        }
    }

    override fun newSession(sessionConfig: OonimkallBridge.SessionConfig): OonimkallBridge.Session {
        lastSessionConfig = sessionConfig
        return Session()
    }

    inner class Session : OonimkallBridge.Session {
        override fun submitMeasurement(measurement: String): OonimkallBridge.SubmitMeasurementResults = submitMeasurementMock!!(measurement)

        override fun checkIn(config: OonimkallBridge.CheckInConfig): OonimkallBridge.CheckInResults = checkInMock!!(config)

        override fun httpDo(request: OonimkallBridge.HTTPRequest): OonimkallBridge.HTTPResponse = httpDoMock!!(request)

        override fun close() {}
    }
}

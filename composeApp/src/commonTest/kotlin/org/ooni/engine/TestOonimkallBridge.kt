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

    var submitMeasurement: ((String) -> OonimkallBridge.SubmitMeasurementResults)? = null
    var checkIn: ((OonimkallBridge.CheckInConfig) -> OonimkallBridge.CheckInResults)? = null
    var httpDo: ((OonimkallBridge.HTTPRequest) -> OonimkallBridge.HTTPResponse)? = null

    // Base implementation

    override fun startTask(settingsSerialized: String): OonimkallBridge.Task {
        lastStartTaskSettingsSerialized = settingsSerialized
        return object : OonimkallBridge.Task {
            override fun interrupt() {}

            override fun isDone() = nextEvents.isEmpty()

            override fun waitForNextEvent(): String {
                return nextEvents.removeAt(0)
            }
        }
    }

    override fun newSession(sessionConfig: OonimkallBridge.SessionConfig): OonimkallBridge.Session {
        lastSessionConfig = sessionConfig
        return Session()
    }

    class Session : OonimkallBridge.Session {
        override fun submitMeasurement(measurement: String): OonimkallBridge.SubmitMeasurementResults {
            return submitMeasurement(measurement)
        }

        override fun checkIn(config: OonimkallBridge.CheckInConfig): OonimkallBridge.CheckInResults {
            return checkIn(config)
        }

        override fun httpDo(request: OonimkallBridge.HTTPRequest): OonimkallBridge.HTTPResponse {
            return httpDo(request)
        }
    }
}

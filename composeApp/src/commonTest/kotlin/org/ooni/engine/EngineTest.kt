package org.ooni.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.EnginePreferences
import org.ooni.engine.models.Failure
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.TaskEvent
import org.ooni.engine.models.TaskLogLevel
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TaskSettings
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngineTest {
    private val json = Dependencies.buildJson()
    private val networkTypeFinder = NetworkTypeFinder { NetworkType.NoInternet }

    @Test
    fun startTaskAndGetEvents() =
        runTest {
            val bridge = TestOonimkallBridge()
            bridge.addNextEvents("""{"key":"status.started","value":{}}""")
            val engine = buildEngine(bridge)

            val events = engine.startTask(
                name = "web_connectivity",
                inputs = listOf("https://ooni.org"),
                taskOrigin = TaskOrigin.OoniRun,
                descriptorId = null,
            ).toList()

            assertEquals(1, events.size)
            assertEquals(TaskEvent.Started, events.first())

            val settings =
                json.decodeFromString<TaskSettings>(bridge.lastStartTaskSettingsSerialized!!)
            assertEquals("web_connectivity", settings.name)
            assertEquals(listOf("https://ooni.org"), settings.inputs)
            assertEquals(NetworkType.NoInternet, settings.annotations.networkType)
        }

    @Test
    fun httpDoWithException() =
        runTest {
            val bridge = TestOonimkallBridge()
            val exception = IllegalStateException("failure")
            bridge.httpDoMock = { throw exception }
            val engine = buildEngine(bridge)

            val result = engine.httpDo("GET", "https://example.org")

            assertTrue(result is Failure)
            assertEquals(exception, result.reason.cause)
        }

    private fun buildEngine(bridge: OonimkallBridge) =
        Engine(
            bridge = bridge,
            json = json,
            baseFilePath = "",
            cacheDir = "",
            taskEventMapper = TaskEventMapper(networkTypeFinder, json),
            networkTypeFinder = networkTypeFinder,
            isBatteryCharging = { true },
            platformInfo = object : PlatformInfo {
                override val buildName: String = "1"
                override val buildNumber: String = "1"
                override val platform = Platform.Ios
                override val osVersion = "1"
                override val model = "test"
                override val needsToRequestNotificationsPermission = false
            },
            getEnginePreferences = {
                EnginePreferences(
                    enabledWebCategories = emptyList(),
                    taskLogLevel = TaskLogLevel.Info,
                    uploadResults = false,
                    proxy = null,
                    maxRuntime = null,
                )
            },
            backgroundDispatcher = Dispatchers.Unconfined,
        )
}

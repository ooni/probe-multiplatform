package org.ooni.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.TaskEvent
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TaskSettings
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class EngineTest {
    private val json = Dependencies.buildJson()
    private val networkTypeFinder = NetworkTypeFinder { NetworkType.NoInternet }

    @Test
    fun startTaskAndGetEvents() =
        runTest {
            val bridge = TestOonimkallBridge()
            bridge.addNextEvents("""{"key":"status.started","value":{}}""")
            val engine = buildEngine(bridge)

            val events =
                engine.startTask(
                    name = "web_connectivity",
                    inputs = listOf("https://ooni.org"),
                    TaskOrigin.OoniRun,
                ).toList()

            assertEquals(1, events.size)
            assertEquals(TaskEvent.Started, events.first())

            val settings = json.decodeFromString<TaskSettings>(bridge.lastStartTaskSettingsSerialized!!)
            assertEquals("web_connectivity", settings.name)
            assertEquals(listOf("https://ooni.org"), settings.inputs)
            assertEquals(NetworkType.NoInternet, settings.annotations.networkType)
        }

    private fun buildEngine(bridge: OonimkallBridge) =
        Engine(
            bridge = bridge,
            json = json,
            baseFilePath = "",
            cacheDir = "",
            taskEventMapper = TaskEventMapper(networkTypeFinder, json),
            networkTypeFinder = networkTypeFinder,
            platformInfo =
                object : PlatformInfo {
                    override val version = "1"
                    override val platform = Platform.Ios
                    override val osVersion = "1"
                    override val model = "test"
                },
            backgroundDispatcher = Dispatchers.Default,
        )
}

package org.ooni.probe.shared

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class InstanceManagerTest {
    var subject: InstanceManager? = null

    @Before
    fun setUp() {
        subject = InstanceManager(platformInfo)
    }

    @After
    fun tearDown() {
        subject?.shutdown()
        subject = null
    }

    @Test
    fun emptyArgs() =
        runTest {
            val urls = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                subject!!
                    .observeUrls()
                    .toList(urls)
            }

            subject!!.initialize(emptyArray())

            assertEquals(0, urls.size)
        }

    @Test
    fun emptyString() =
        runTest {
            val urls = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                subject!!
                    .observeUrls()
                    .toList(urls)
            }

            subject!!.initialize(arrayOf(""))

            assertEquals(0, urls.size)
        }

    @Test
    fun blankString() =
        runTest {
            val urls = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                subject!!
                    .observeUrls()
                    .toList(urls)
            }

            subject!!.initialize(arrayOf(" "))

            assertEquals(0, urls.size)
        }

    @Test
    fun url() =
        runTest {
            val urls = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                subject!!
                    .observeUrls()
                    .toList(urls)
            }

            val url = "https://example.org"
            subject!!.initialize(arrayOf(url))

            assertEquals(1, urls.size)
            assertEquals(url, urls.first())
        }

    private val platformInfo = PlatformInfo(
        buildName = "",
        buildNumber = "",
        platform = Platform.Desktop("Windows"),
        osVersion = "",
        model = "",
        requestNotificationsPermission = false,
        sentryDsn = "",
    )
}

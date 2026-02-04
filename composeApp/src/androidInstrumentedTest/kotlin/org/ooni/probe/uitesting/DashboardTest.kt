package org.ooni.probe.uitesting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_Title
import ooniprobe.composeapp.generated.resources.Dashboard_TestsMoved_Description
import ooniprobe.composeapp.generated.resources.Res
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.uitesting.helpers.disableRefreshArticles
import org.ooni.probe.uitesting.helpers.isNewsMediaScan
import org.ooni.probe.uitesting.helpers.onNodeWithText
import org.ooni.probe.uitesting.helpers.preferences
import org.ooni.probe.uitesting.helpers.skipOnboarding
import org.ooni.probe.uitesting.helpers.start
import org.ooni.probe.uitesting.helpers.wait
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class DashboardTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() =
        runTest {
            skipOnboarding()
        }

    @Test
    fun testsMovedNotice() =
        runTest {
            disableRefreshArticles()
            preferences.setValueByKey(SettingsKey.TESTS_MOVED_NOTICE, false)
            start()

            with(compose) {
                onNodeWithText(Res.string.Dashboard_TestsMoved_Description).assertIsDisplayed()
            }
        }

    @Test
    fun news() =
        runTest {
            if (isNewsMediaScan) return@runTest

            preferences.setValueByKey(SettingsKey.LAST_ARTICLES_REFRESH, 0L)
            start()

            with(compose) {
                wait(timeout = 1.minutes) {
                    onNodeWithText(Res.string.Dashboard_Articles_Title).isDisplayed()
                }
            }
        }
}

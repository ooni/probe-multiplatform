package org.ooni.probe.uitesting

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.Common_Expand
import ooniprobe.composeapp.generated.resources.Dashboard_LastResults_SeeResults
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_RunButton_Label
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_SelectNone
import ooniprobe.composeapp.generated.resources.OONIRun_Run
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Test_HTTPHeaderFieldManipulation_Fullname
import ooniprobe.composeapp.generated.resources.Test_Psiphon_Fullname
import ooniprobe.composeapp.generated.resources.Test_Signal_Fullname
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.probe.data.models.OoniTest
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.uitesting.helpers.clickOnContentDescription
import org.ooni.probe.uitesting.helpers.clickOnText
import org.ooni.probe.uitesting.helpers.disableRefreshArticles
import org.ooni.probe.uitesting.helpers.getOoniDescriptor
import org.ooni.probe.uitesting.helpers.isNewsMediaScan
import org.ooni.probe.uitesting.helpers.isOoni
import org.ooni.probe.uitesting.helpers.onNodeWithText
import org.ooni.probe.uitesting.helpers.preferences
import org.ooni.probe.uitesting.helpers.setupMockedEngine
import org.ooni.probe.uitesting.helpers.skipOnboarding
import org.ooni.probe.uitesting.helpers.start
import org.ooni.probe.uitesting.helpers.wait

@RunWith(AndroidJUnit4::class)
class RunningTestsTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() =
        runTest {
            skipOnboarding()
            disableRefreshArticles()
            setupMockedEngine()
            preferences.setValueByKey(SettingsKey.UPLOAD_RESULTS, true)
            start()
        }

    @Test
    fun signal() =
        runTest {
            if (!isOoni) return@runTest
            val imTitle = getOoniDescriptor(OoniTest.InstantMessaging).title()
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                clickOnContentDescription(getString(Res.string.Common_Expand) + " " + imTitle)
                clickOnText(Res.string.Test_Signal_Fullname)
                clickOnRunButton(1)

                clickOnText(Res.string.Dashboard_LastResults_SeeResults)
                clickOnText(imTitle)
                wait { onNodeWithText(Res.string.Test_Signal_Fullname).isDisplayed() }
            }
        }

    @Test
    fun psiphon() =
        runTest {
            if (!isOoni) return@runTest
            val circTitle = getOoniDescriptor(OoniTest.Circumvention).title()
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                clickOnContentDescription(getString(Res.string.Common_Expand) + " " + circTitle)
                clickOnText(Res.string.Test_Psiphon_Fullname)
                clickOnRunButton(1)

                clickOnText(Res.string.Dashboard_LastResults_SeeResults)

                clickOnText(circTitle)
                wait { onNodeWithText(Res.string.Test_Psiphon_Fullname).isDisplayed() }
            }
        }

    @Test
    fun httpHeader() =
        runTest {
            if (!isOoni) return@runTest
            val perfTitle = getOoniDescriptor(OoniTest.Performance).title()
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                clickOnContentDescription(getString(Res.string.Common_Expand) + " " + perfTitle)
                clickOnText(
                    getString(Res.string.Test_HTTPHeaderFieldManipulation_Fullname).take(16),
                    substring = true,
                )
                clickOnRunButton(1)

                clickOnText(Res.string.Dashboard_LastResults_SeeResults)

                clickOnText(perfTitle)
                val testNameSubstring =
                    getString(Res.string.Test_HTTPHeaderFieldManipulation_Fullname).take(16)
                wait { onNodeWithText(testNameSubstring, substring = true).isDisplayed() }
            }
        }

    @Test
    fun stunReachability() =
        runTest {
            if (!isOoni) return@runTest
            val expTitle = getOoniDescriptor(OoniTest.Experimental).title()
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                clickOnContentDescription(getString(Res.string.Common_Expand) + " " + expTitle)
                clickOnText("stunreachability", substring = true)
                clickOnRunButton(1)

                clickOnText(Res.string.Dashboard_LastResults_SeeResults)

                clickOnText(expTitle)
                wait { onAllNodesWithText("stunreachability").onFirst().isDisplayed() }
            }
        }

    @Test
    fun trustedInternationalMedia() =
        runTest {
            if (!isNewsMediaScan) return@runTest
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                clickOnText("Trusted International Media")
                clickOnRunButton(1)

                clickOnText(Res.string.Dashboard_LastResults_SeeResults)

                clickOnText("Trusted International Media")
                wait { onNodeWithText("https://www.dw.com").isDisplayed() }
            }
        }

    private suspend fun ComposeTestRule.clickOnRunButton(quantity: Int) {
        clickOnText(
            getPluralString(
                Res.plurals.Dashboard_RunTests_RunButton_Label,
                quantity,
                quantity,
            ),
        )
    }
}

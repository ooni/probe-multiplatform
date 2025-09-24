package org.ooni.probe.uitesting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.CategoryCode_ANON_Name
import ooniprobe.composeapp.generated.resources.Common_Back
import ooniprobe.composeapp.generated.resources.Modal_OK
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Advanced_DebugLogs
import ooniprobe.composeapp.generated.resources.Settings_Advanced_Label
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_ChargingOnly
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_WiFiOnly
import ooniprobe.composeapp.generated.resources.Settings_Privacy_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_SendCrashReports
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Label
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Psiphon
import ooniprobe.composeapp.generated.resources.Settings_Results_DeleteOldResults
import ooniprobe.composeapp.generated.resources.Settings_Results_DeleteOldResultsThreshold
import ooniprobe.composeapp.generated.resources.Settings_Sharing_UploadResults
import ooniprobe.composeapp.generated.resources.Settings_TestOptions_Label
import ooniprobe.composeapp.generated.resources.Settings_Title
import ooniprobe.composeapp.generated.resources.Settings_WarmVPNInUse_Label
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Description
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Label
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.uitesting.helpers.clickOnContentDescription
import org.ooni.probe.uitesting.helpers.clickOnText
import org.ooni.probe.uitesting.helpers.isCrashReportingEnabled
import org.ooni.probe.uitesting.helpers.isOoni
import org.ooni.probe.uitesting.helpers.preferences
import org.ooni.probe.uitesting.helpers.skipOnboarding
import org.ooni.probe.uitesting.helpers.start
import org.ooni.probe.uitesting.helpers.wait

@RunWith(AndroidJUnit4::class)
class SettingsTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() =
        runTest {
            skipOnboarding()
            start()
        }

    @Test
    fun testOptions_runAutoTests() =
        runTest {
            preferences.setValuesByKey(
                listOf(
                    SettingsKey.UPLOAD_RESULTS to false,
                    SettingsKey.AUTOMATED_TESTING_ENABLED to false,
                    SettingsKey.AUTOMATED_TESTING_WIFIONLY to false,
                    SettingsKey.AUTOMATED_TESTING_CHARGING to false,
                ),
            )

            with(compose) {
                clickOnText(Res.string.Settings_Title)
                clickOnText(Res.string.Settings_TestOptions_Label)

                clickOnText(Res.string.Settings_Sharing_UploadResults)
                wait { preferences.getValueByKey(SettingsKey.UPLOAD_RESULTS).first() == true }

                clickOnText(Res.string.Settings_AutomatedTesting_RunAutomatically)
                wait {
                    preferences.getValueByKey(SettingsKey.AUTOMATED_TESTING_ENABLED).first() == true
                }

                clickOnText(Res.string.Settings_AutomatedTesting_RunAutomatically_WiFiOnly)
                clickOnText(Res.string.Settings_AutomatedTesting_RunAutomatically_ChargingOnly)

                wait {
                    preferences
                        .getValueByKey(SettingsKey.AUTOMATED_TESTING_WIFIONLY)
                        .first() == true &&
                        preferences
                            .getValueByKey(SettingsKey.AUTOMATED_TESTING_CHARGING)
                            .first() == true
                }
            }
        }

    @Test
    fun testOptions_websiteCategories() =
        runTest {
            if (!isOoni) return@runTest
            preferences.setValuesByKey(
                WebConnectivityCategory.entries.mapNotNull {
                    it.settingsKey?.let { key -> key to false }
                },
            )

            with(compose) {
                clickOnText(Res.string.Settings_Title)
                clickOnText(Res.string.Settings_TestOptions_Label)

                onNodeWithText(getString(Res.string.Settings_Websites_Categories_Description, 0))
                    .assertIsDisplayed()
                clickOnText(Res.string.Settings_Websites_Categories_Label)

                clickOnText(Res.string.CategoryCode_ANON_Name)
                clickOnContentDescription(Res.string.Common_Back)

                onNodeWithText(getString(Res.string.Settings_Websites_Categories_Description, 1))
                    .assertIsDisplayed()

                wait {
                    preferences
                        .getValueByKey(WebConnectivityCategory.ANON.settingsKey!!)
                        .first() == true
                }
            }
        }

    @Test
    fun privacy() =
        runTest {
            if (!isCrashReportingEnabled) return@runTest

            preferences.setValuesByKey(
                listOf(
                    SettingsKey.SEND_CRASH to false,
                ),
            )

            with(compose) {
                clickOnText(Res.string.Settings_Title)
                clickOnText(Res.string.Settings_Privacy_Label)
                clickOnText(Res.string.Settings_Privacy_SendCrashReports)

                wait {
                    preferences.getValueByKey(SettingsKey.SEND_CRASH).first() == true
                }
            }
        }

    @Test
    fun proxy() =
        runTest {
            preferences.setValuesByKey(
                listOf(SettingsKey.PROXY_SELECTED to ProxyOption.None.value),
            )

            with(compose) {
                clickOnText(Res.string.Settings_Title)
                clickOnText(Res.string.Settings_Proxy_Label)
                clickOnText(Res.string.Settings_Proxy_Psiphon)

                wait {
                    preferences
                        .getValueByKey(SettingsKey.PROXY_SELECTED)
                        .first() == ProxyOption.Psiphon.value
                }
            }
        }

    @Test
    fun advanced() =
        runTest {
            preferences.setValuesByKey(
                listOf(
                    SettingsKey.DEBUG_LOGS to false,
                    SettingsKey.WARN_VPN_IN_USE to false,
                ),
            )

            with(compose) {
                clickOnText(Res.string.Settings_Title)
                clickOnText(Res.string.Settings_Advanced_Label)

                clickOnText(Res.string.Settings_Advanced_DebugLogs)
                wait { preferences.getValueByKey(SettingsKey.DEBUG_LOGS).first() == true }

                clickOnText(Res.string.Settings_WarmVPNInUse_Label)
                wait { preferences.getValueByKey(SettingsKey.WARN_VPN_IN_USE).first() == true }

                clickOnText(Res.string.Settings_Results_DeleteOldResults)
                wait { preferences.getValueByKey(SettingsKey.DELETE_OLD_RESULTS).first() == true }

                clickOnText(Res.string.Settings_Results_DeleteOldResultsThreshold)
                onNodeWithTag("NumberPickerField").performTextInput("8")
                clickOnText(Res.string.Modal_OK)
                wait { preferences.getValueByKey(SettingsKey.DELETE_OLD_RESULTS_THRESHOLD).first() == 8 }
            }
        }
}

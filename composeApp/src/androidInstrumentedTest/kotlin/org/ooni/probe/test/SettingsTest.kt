package org.ooni.probe.test

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.ProxyProtocol
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.test.helpers.CleanTestRule
import org.ooni.probe.test.helpers.FlakyTestRule
import org.ooni.probe.test.helpers.clickOnContentDescription
import org.ooni.probe.test.helpers.clickOnText
import org.ooni.probe.test.helpers.preferences
import org.ooni.probe.test.helpers.skipOnboarding
import org.ooni.probe.test.helpers.start
import org.ooni.probe.test.helpers.wait

@RunWith(AndroidJUnit4::class)
class SettingsTest {
    @get:Rule
    val clean = CleanTestRule()

    @get:Rule
    val flakyTestRule = FlakyTestRule()

    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() =
        runTest {
            skipOnboarding()
            start()
        }

    @Test
    fun notifications() =
        runTest {
            assertTrue(preferences.getValueByKey(SettingsKey.NOTIFICATIONS_ENABLED).first() != true)

            with(compose) {
                clickOnText("Settings")
                clickOnText("Notifications")
                clickOnText("Enabled")

                wait {
                    preferences.getValueByKey(SettingsKey.NOTIFICATIONS_ENABLED).first() == true
                }
            }
        }

    @Test
    fun testOptions_runAutoTests() =
        runTest {
            preferences.setValuesByKey(
                listOf(
                    SettingsKey.AUTOMATED_TESTING_ENABLED to false,
                    SettingsKey.AUTOMATED_TESTING_WIFIONLY to false,
                    SettingsKey.AUTOMATED_TESTING_CHARGING to false,
                ),
            )

            with(compose) {
                clickOnText("Settings")
                clickOnText("Test options")

                clickOnText("Run tests automatically")
                clickOnText("Only on WiFi")
                clickOnText("Only while charging")

                wait {
                    preferences.getValueByKey(SettingsKey.AUTOMATED_TESTING_ENABLED)
                        .first() == true &&
                        preferences.getValueByKey(SettingsKey.AUTOMATED_TESTING_WIFIONLY)
                            .first() == true &&
                        preferences.getValueByKey(SettingsKey.AUTOMATED_TESTING_CHARGING)
                            .first() == true
                }
            }
        }

    @Test
    fun testOptions_websiteCategories() =
        runTest {
            preferences.setValuesByKey(
                WebConnectivityCategory.entries.mapNotNull {
                    it.settingsKey?.let { key -> key to false }
                },
            )

            with(compose) {
                clickOnText("Settings")
                clickOnText("Test options")

                onNodeWithText("0 categories enabled").assertIsDisplayed()
                clickOnText("Website categories to test")

                clickOnText("Circumvention tools")
                clickOnContentDescription("Back")

                clickOnText("1 categories enabled")

                wait {
                    preferences.getValueByKey(WebConnectivityCategory.ANON.settingsKey!!)
                        .first() == true
                }
            }
        }

    @Test
    fun privacy() =
        runTest {
            preferences.setValuesByKey(
                listOf(
                    SettingsKey.UPLOAD_RESULTS to false,
                    SettingsKey.SEND_CRASH to false,
                ),
            )

            with(compose) {
                clickOnText("Settings")
                clickOnText("Privacy")
                clickOnText("Automatically Publish Results")
                clickOnText("Send crash reports")

                wait {
                    preferences.getValueByKey(SettingsKey.UPLOAD_RESULTS).first() == true &&
                        preferences.getValueByKey(SettingsKey.SEND_CRASH).first() == true
                }
            }
        }

    @Test
    fun proxy() =
        runTest {
            preferences.setValuesByKey(
                listOf(
                    SettingsKey.PROXY_PROTOCOL to "NONE",
                ),
            )

            with(compose) {
                clickOnText("Settings")
                clickOnText("OONI backend proxy")
                clickOnText("Psiphon")

                wait {
                    preferences.getValueByKey(SettingsKey.PROXY_PROTOCOL)
                        .first() == ProxyProtocol.PSIPHON.value
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
                clickOnText("Settings")
                clickOnText("Advanced")
                clickOnText("Debug logs")
                clickOnText("Warn when VPN is in use")

                wait {
                    preferences.getValueByKey(SettingsKey.DEBUG_LOGS).first() == true &&
                        preferences.getValueByKey(SettingsKey.WARN_VPN_IN_USE).first() == true
                }
            }
        }
}

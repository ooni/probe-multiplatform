package org.ooni.probe.domain

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_Delete
import ooniprobe.composeapp.generated.resources.Modal_DoYouWantToDeleteAllTests
import ooniprobe.composeapp.generated.resources.Modal_EnableNotifications_Paragraph
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_About_Label
import ooniprobe.composeapp.generated.resources.Settings_Advanced_DebugLogs
import ooniprobe.composeapp.generated.resources.Settings_Advanced_Label
import ooniprobe.composeapp.generated.resources.Settings_Advanced_RecentLogs
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_ChargingOnly
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_Footer
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_WiFiOnly
import ooniprobe.composeapp.generated.resources.Settings_Notifications_Enabled
import ooniprobe.composeapp.generated.resources.Settings_Notifications_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_SendCrashReports
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Label
import ooniprobe.composeapp.generated.resources.Settings_SendEmail_Label
import ooniprobe.composeapp.generated.resources.Settings_Sharing_UploadResults
import ooniprobe.composeapp.generated.resources.Settings_Storage_Clear
import ooniprobe.composeapp.generated.resources.Settings_Storage_Label
import ooniprobe.composeapp.generated.resources.Settings_TestOptions_Label
import ooniprobe.composeapp.generated.resources.Settings_WarmVPNInUse_Label
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Description
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Label
import ooniprobe.composeapp.generated.resources.Settings_Websites_MaxRuntime
import ooniprobe.composeapp.generated.resources.Settings_Websites_MaxRuntimeEnabled
import ooniprobe.composeapp.generated.resources.advanced
import ooniprobe.composeapp.generated.resources.auto_test_not_uploaded_limit
import ooniprobe.composeapp.generated.resources.ic_settings
import ooniprobe.composeapp.generated.resources.notifications
import ooniprobe.composeapp.generated.resources.outline_info
import ooniprobe.composeapp.generated.resources.privacy
import ooniprobe.composeapp.generated.resources.proxy
import ooniprobe.composeapp.generated.resources.send_email
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.PreferenceItemType
import org.ooni.probe.data.models.SettingsCategoryItem
import org.ooni.probe.data.models.SettingsItem
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.ui.settings.category.SettingsDescription
import org.ooni.probe.ui.shared.formatDataUsage
import org.ooni.probe.ui.shared.shortFormat
import kotlin.time.Duration.Companion.seconds

class GetSettings(
    private val preferencesRepository: PreferenceRepository,
    private val clearStorage: suspend () -> Unit,
    val observeStorageUsed: () -> Flow<Long>,
    private val supportsCrashReporting: Boolean,
) {
    operator fun invoke(): Flow<List<SettingsCategoryItem>> {
        return combine(
            preferencesRepository.allSettings(
                WebConnectivityCategory.entries.mapNotNull { it.settingsKey } + listOf(
                    SettingsKey.UPLOAD_RESULTS,
                    SettingsKey.AUTOMATED_TESTING_ENABLED,
                    SettingsKey.AUTOMATED_TESTING_NOT_UPLOADED_LIMIT,
                    SettingsKey.MAX_RUNTIME_ENABLED,
                    SettingsKey.MAX_RUNTIME,
                ),
            ),
            observeStorageUsed(),
        ) { preferences, storageUsed ->
            val enabledCategoriesCount =
                WebConnectivityCategory.entries.count { preferences[it.settingsKey] == true }
            buildSettings(
                uploadResultsEnabled = preferences[SettingsKey.UPLOAD_RESULTS] == true,
                autoRunEnabled = preferences[SettingsKey.AUTOMATED_TESTING_ENABLED] == true,
                autoRunNotUploadedLimit =
                    preferences[SettingsKey.AUTOMATED_TESTING_NOT_UPLOADED_LIMIT] as? Int,
                enabledCategoriesCount = enabledCategoriesCount,
                maxRuntimeEnabled = preferences[SettingsKey.MAX_RUNTIME_ENABLED] == true,
                maxRuntime = preferences[SettingsKey.MAX_RUNTIME] as? Int,
                storageUsed = storageUsed,
                supportsCrashReporting = supportsCrashReporting,
            )
        }
    }

    private fun buildSettings(
        uploadResultsEnabled: Boolean,
        autoRunEnabled: Boolean,
        autoRunNotUploadedLimit: Int?,
        enabledCategoriesCount: Int,
        maxRuntimeEnabled: Boolean,
        maxRuntime: Int?,
        storageUsed: Long,
        supportsCrashReporting: Boolean = false,
    ): List<SettingsCategoryItem> {
        return listOfNotNull(
            SettingsCategoryItem(
                icon = Res.drawable.notifications,
                title = Res.string.Settings_Notifications_Label,
                route = PreferenceCategoryKey.NOTIFICATIONS,
                settings = listOf(
                    SettingsItem(
                        title = Res.string.Settings_Notifications_Enabled,
                        key = SettingsKey.NOTIFICATIONS_ENABLED,
                        type = PreferenceItemType.SWITCH,
                    ),
                ),
                footerContent = {
                    SettingsDescription(
                        Res.string.Modal_EnableNotifications_Paragraph,
                    )
                },
            ),
            SettingsCategoryItem(
                icon = Res.drawable.ic_settings,
                title = Res.string.Settings_TestOptions_Label,
                route = PreferenceCategoryKey.TEST_OPTIONS,
                settings = listOf(
                    SettingsItem(
                        title = Res.string.Settings_Sharing_UploadResults,
                        key = SettingsKey.UPLOAD_RESULTS,
                        type = PreferenceItemType.SWITCH,
                    ),
                    SettingsItem(
                        title = Res.string.Settings_AutomatedTesting_RunAutomatically,
                        key = SettingsKey.AUTOMATED_TESTING_ENABLED,
                        type = PreferenceItemType.SWITCH,
                        enabled = uploadResultsEnabled,
                    ),
                    SettingsItem(
                        title = Res.string.Settings_AutomatedTesting_RunAutomatically_WiFiOnly,
                        key = SettingsKey.AUTOMATED_TESTING_WIFIONLY,
                        type = PreferenceItemType.SWITCH,
                        enabled = autoRunEnabled,
                    ),
                    SettingsItem(
                        title = Res.string.Settings_AutomatedTesting_RunAutomatically_ChargingOnly,
                        key = SettingsKey.AUTOMATED_TESTING_CHARGING,
                        type = PreferenceItemType.SWITCH,
                        enabled = autoRunEnabled,
                    ),
                    SettingsItem(
                        title = Res.string.auto_test_not_uploaded_limit,
                        key = SettingsKey.AUTOMATED_TESTING_NOT_UPLOADED_LIMIT,
                        type = PreferenceItemType.INT,
                        enabled = autoRunEnabled,
                        supportingContent = {
                            val value = (
                                autoRunNotUploadedLimit
                                    ?: BootstrapPreferences.NOT_UPLOADED_LIMIT_DEFAULT
                            ).coerceAtLeast(1)
                            Text(value.toString())
                        },
                    ),
                    SettingsCategoryItem(
                        title = Res.string.Settings_Websites_Categories_Label,
                        route = PreferenceCategoryKey.WEBSITES_CATEGORIES,
                        supportingContent = {
                            Text(
                                stringResource(
                                    Res.string.Settings_Websites_Categories_Description,
                                    enabledCategoriesCount,
                                ),
                            )
                        },
                        settings = WebConnectivityCategory.entries
                            .mapNotNull { cat ->
                                SettingsItem(
                                    icon = cat.icon,
                                    title = cat.title,
                                    supportingContent = { Text(stringResource(cat.description)) },
                                    key = cat.settingsKey ?: return@mapNotNull null,
                                    type = PreferenceItemType.SWITCH,
                                )
                            },
                    ),
                    SettingsItem(
                        title = Res.string.Settings_Websites_MaxRuntimeEnabled,
                        key = SettingsKey.MAX_RUNTIME_ENABLED,
                        type = PreferenceItemType.SWITCH,
                    ),
                    SettingsItem(
                        title = Res.string.Settings_Websites_MaxRuntime,
                        key = SettingsKey.MAX_RUNTIME,
                        type = PreferenceItemType.INT,
                        enabled = maxRuntimeEnabled,
                        supportingContent = {
                            maxRuntime?.let {
                                Text(it.coerceAtLeast(0).seconds.shortFormat())
                            }
                        },
                    ),
                ),
                footerContent = {
                    SettingsDescription(
                        Res.string.Settings_AutomatedTesting_RunAutomatically_Footer,
                    )
                },
            ),
            if (supportsCrashReporting) {
                SettingsCategoryItem(
                    icon = Res.drawable.privacy,
                    title = Res.string.Settings_Privacy_Label,
                    route = PreferenceCategoryKey.PRIVACY,
                    settings = buildList {
                        if (supportsCrashReporting) {
                            add(
                                SettingsItem(
                                    title = Res.string.Settings_Privacy_SendCrashReports,
                                    key = SettingsKey.SEND_CRASH,
                                    type = PreferenceItemType.SWITCH,
                                ),
                            )
                        }
                    },
                )
            } else {
                null
            },
            SettingsCategoryItem(
                icon = Res.drawable.proxy,
                title = Res.string.Settings_Proxy_Label,
                route = PreferenceCategoryKey.PROXY,
            ),
            SettingsCategoryItem(
                icon = Res.drawable.advanced,
                title = Res.string.Settings_Advanced_Label,
                route = PreferenceCategoryKey.ADVANCED,
                settings = listOf(
                    // TODO(aanorbel) : Add language settings when in app language switcher can be implemented
                    SettingsCategoryItem(
                        title = Res.string.Settings_Advanced_RecentLogs,
                        route = PreferenceCategoryKey.SEE_RECENT_LOGS,
                    ),
                    SettingsItem(
                        title = Res.string.Settings_Advanced_DebugLogs,
                        key = SettingsKey.DEBUG_LOGS,
                        type = PreferenceItemType.SWITCH,
                    ),
                    SettingsItem(
                        title = Res.string.Settings_Storage_Label,
                        key = SettingsKey.STORAGE_SIZE,
                        type = PreferenceItemType.BUTTON,
                        supportingContent = {
                            Text(storageUsed.formatDataUsage())
                        },
                        trailingContent = {
                            var showDialog by remember { mutableStateOf(false) }
                            val coroutine = rememberCoroutineScope()

                            if (showDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDialog = false },
                                    text = { Text(stringResource(Res.string.Modal_DoYouWantToDeleteAllTests)) },
                                    confirmButton = {
                                        Button(
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError,
                                            ),
                                            onClick = {
                                                coroutine.launch {
                                                    clearStorage()
                                                    showDialog = false
                                                }
                                                showDialog = false
                                            },
                                        ) {
                                            Text(stringResource(Res.string.Modal_Delete))
                                        }
                                    },
                                    dismissButton = {
                                        OutlinedButton(
                                            onClick = { showDialog = false },
                                        ) {
                                            Text(stringResource(Res.string.Modal_Cancel))
                                        }
                                    },
                                )
                            }

                            Button(
                                onClick = { showDialog = true },
                            ) {
                                Text(stringResource(Res.string.Settings_Storage_Clear))
                            }
                        },
                    ),
                    SettingsItem(
                        title = Res.string.Settings_WarmVPNInUse_Label,
                        key = SettingsKey.WARN_VPN_IN_USE,
                        type = PreferenceItemType.SWITCH,
                    ),
                ),
            ),
            SettingsCategoryItem(
                icon = Res.drawable.send_email,
                title = Res.string.Settings_SendEmail_Label,
                route = PreferenceCategoryKey.SEND_EMAIL,
            ),
            SettingsCategoryItem(
                icon = Res.drawable.outline_info,
                title = Res.string.Settings_About_Label,
                route = PreferenceCategoryKey.ABOUT_OONI,
            ),
        )
    }
}

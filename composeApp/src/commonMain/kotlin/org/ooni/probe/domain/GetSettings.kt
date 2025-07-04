package org.ooni.probe.domain

import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_Delete
import ooniprobe.composeapp.generated.resources.Modal_DoYouWantToDeleteAllTests
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_About_Label
import ooniprobe.composeapp.generated.resources.Settings_Advanced_DebugLogs
import ooniprobe.composeapp.generated.resources.Settings_Advanced_Label
import ooniprobe.composeapp.generated.resources.Settings_Advanced_RecentLogs
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_ChargingOnly
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_Description
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_Footer
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_WiFiOnly
import ooniprobe.composeapp.generated.resources.Settings_Language_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_SendCrashReports
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Label
import ooniprobe.composeapp.generated.resources.Settings_Sharing_UploadResults
import ooniprobe.composeapp.generated.resources.Settings_Sharing_UploadResults_Description
import ooniprobe.composeapp.generated.resources.Settings_Storage_Clear
import ooniprobe.composeapp.generated.resources.Settings_Storage_Label
import ooniprobe.composeapp.generated.resources.Settings_Support_Label
import ooniprobe.composeapp.generated.resources.Settings_TestOptions_Label
import ooniprobe.composeapp.generated.resources.Settings_WarmVPNInUse_Label
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Description
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Label
import ooniprobe.composeapp.generated.resources.Settings_Websites_MaxRuntimeEnabled_Description
import ooniprobe.composeapp.generated.resources.Settings_Websites_MaxRuntimeEnabled_New
import ooniprobe.composeapp.generated.resources.Settings_Websites_MaxRuntime_New
import ooniprobe.composeapp.generated.resources.advanced
import ooniprobe.composeapp.generated.resources.ic_language
import ooniprobe.composeapp.generated.resources.ic_settings
import ooniprobe.composeapp.generated.resources.ic_support
import ooniprobe.composeapp.generated.resources.outline_info
import ooniprobe.composeapp.generated.resources.privacy
import ooniprobe.composeapp.generated.resources.proxy
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.PreferenceItemType
import org.ooni.probe.data.models.SettingsCategoryItem
import org.ooni.probe.data.models.SettingsItem
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.ui.settings.category.SettingsDescription
import org.ooni.probe.ui.settings.donate.DONATE_SETTINGS_ITEM
import org.ooni.probe.ui.shared.formatDataUsage
import org.ooni.probe.ui.shared.shortFormat
import kotlin.time.Duration.Companion.seconds

class GetSettings(
    private val preferencesRepository: PreferenceRepository,
    private val clearStorage: suspend (Boolean) -> Unit,
    val observeStorageUsed: () -> Flow<Long>,
    private val supportsCrashReporting: Boolean,
    private val knownNetworkType: Boolean,
    private val knownBatteryState: Boolean,
    private val supportsInAppLanguage: Boolean,
) {
    operator fun invoke(): Flow<List<SettingsCategoryItem>> {
        return combine(
            preferencesRepository.allSettings(
                WebConnectivityCategory.entries.mapNotNull { it.settingsKey } + listOf(
                    SettingsKey.UPLOAD_RESULTS,
                    SettingsKey.AUTOMATED_TESTING_ENABLED,
                    SettingsKey.MAX_RUNTIME_ENABLED,
                    SettingsKey.MAX_RUNTIME,
                ),
            ),
            observeStorageUsed(),
        ) { preferences, storageUsed ->
            val enabledCategoriesCount =
                WebConnectivityCategory.entries.count { preferences[it.settingsKey] == true }
            buildSettings(
                hasWebsitesDescriptor = OrganizationConfig.hasWebsitesDescriptor,
                autoRunEnabled = preferences[SettingsKey.AUTOMATED_TESTING_ENABLED] == true,
                enabledCategoriesCount = enabledCategoriesCount,
                maxRuntimeEnabled = preferences[SettingsKey.MAX_RUNTIME_ENABLED] == true,
                maxRuntime = preferences[SettingsKey.MAX_RUNTIME] as? Int,
                storageUsed = storageUsed,
                supportsCrashReporting = supportsCrashReporting,
            )
        }
    }

    private fun buildSettings(
        hasWebsitesDescriptor: Boolean,
        autoRunEnabled: Boolean,
        enabledCategoriesCount: Int,
        maxRuntimeEnabled: Boolean,
        maxRuntime: Int?,
        storageUsed: Long,
        supportsCrashReporting: Boolean = false,
    ): List<SettingsCategoryItem> {
        return listOfNotNull(
            SettingsCategoryItem(
                icon = Res.drawable.ic_settings,
                title = Res.string.Settings_TestOptions_Label,
                route = PreferenceCategoryKey.TEST_OPTIONS,
                settings = listOfNotNull(
                    SettingsItem(
                        title = Res.string.Settings_Sharing_UploadResults,
                        key = SettingsKey.UPLOAD_RESULTS,
                        type = PreferenceItemType.SWITCH,
                        supportingContent = {
                            Text(
                                stringResource(Res.string.Settings_Sharing_UploadResults_Description),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    ),
                    SettingsItem(
                        title = Res.string.Settings_AutomatedTesting_RunAutomatically,
                        key = SettingsKey.AUTOMATED_TESTING_ENABLED,
                        type = PreferenceItemType.SWITCH,
                        supportingContent = {
                            Text(
                                stringResource(Res.string.Settings_AutomatedTesting_RunAutomatically_Description),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    ),
                    if (autoRunEnabled && knownNetworkType) {
                        SettingsItem(
                            title = Res.string.Settings_AutomatedTesting_RunAutomatically_WiFiOnly,
                            key = SettingsKey.AUTOMATED_TESTING_WIFIONLY,
                            type = PreferenceItemType.SWITCH,
                            enabled = autoRunEnabled,
                            indentation = 1,
                        )
                    } else {
                        null
                    },
                    if (autoRunEnabled && knownBatteryState) {
                        SettingsItem(
                            title = Res.string.Settings_AutomatedTesting_RunAutomatically_ChargingOnly,
                            key = SettingsKey.AUTOMATED_TESTING_CHARGING,
                            type = PreferenceItemType.SWITCH,
                            enabled = autoRunEnabled,
                            indentation = 1,
                        )
                    } else {
                        null
                    },
                    if (hasWebsitesDescriptor) {
                        SettingsItem(
                            title = Res.string.Settings_Websites_MaxRuntimeEnabled_New,
                            key = SettingsKey.MAX_RUNTIME_ENABLED,
                            type = PreferenceItemType.SWITCH,
                            supportingContent = {
                                Text(
                                    stringResource(Res.string.Settings_Websites_MaxRuntimeEnabled_Description),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            indentation = 0,
                        )
                    } else {
                        null
                    },
                    if (hasWebsitesDescriptor && maxRuntimeEnabled) {
                        SettingsItem(
                            title = Res.string.Settings_Websites_MaxRuntime_New,
                            key = SettingsKey.MAX_RUNTIME,
                            type = PreferenceItemType.INT,
                            supportingContent = {
                                maxRuntime?.let {
                                    Text(it.coerceAtLeast(0).seconds.shortFormat())
                                }
                            },
                            indentation = 1,
                        )
                    } else {
                        null
                    },
                    if (hasWebsitesDescriptor) {
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
                        )
                    } else {
                        null
                    },
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
            if (supportsInAppLanguage) {
                SettingsCategoryItem(
                    icon = Res.drawable.ic_language,
                    title = Res.string.Settings_Language_Label,
                    route = PreferenceCategoryKey.LANGUAGE,
                )
            } else {
                null
            },
            SettingsCategoryItem(
                icon = Res.drawable.advanced,
                title = Res.string.Settings_Advanced_Label,
                route = PreferenceCategoryKey.ADVANCED,
                settings = listOf(
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
                            var showDialog by remember { mutableStateOf(false) }
                            if (showDialog) {
                                ClearStorageDialog(
                                    onClose = { showDialog = false },
                                    fullReset = true,
                                )
                            }

                            Text(
                                storageUsed.formatDataUsage(),
                                modifier = Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = { showDialog = true },
                                ),
                            )
                        },
                        trailingContent = {
                            var showDialog by remember { mutableStateOf(false) }
                            if (showDialog) {
                                ClearStorageDialog(onClose = { showDialog = false })
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
                icon = Res.drawable.ic_support,
                title = Res.string.Settings_Support_Label,
                route = PreferenceCategoryKey.SUPPORT,
            ),
            DONATE_SETTINGS_ITEM,
            SettingsCategoryItem(
                icon = Res.drawable.outline_info,
                title = Res.string.Settings_About_Label,
                route = PreferenceCategoryKey.ABOUT_OONI,
            ),
        )
    }

    @Composable
    private fun ClearStorageDialog(
        onClose: () -> Unit,
        fullReset: Boolean = false,
    ) {
        val coroutine = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { onClose() },
            text = { Text(stringResource(Res.string.Modal_DoYouWantToDeleteAllTests)) },
            confirmButton = {
                var enabled by remember { mutableStateOf(true) }
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    enabled = enabled,
                    onClick = {
                        enabled = false
                        coroutine.launch {
                            clearStorage(fullReset)
                            onClose()
                        }
                    },
                ) {
                    Text(stringResource(Res.string.Modal_Delete))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { onClose() },
                ) {
                    Text(stringResource(Res.string.Modal_Cancel))
                }
            },
        )
    }
}

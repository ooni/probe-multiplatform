package org.ooni.probe.ui.settings.donate

import androidx.compose.runtime.Composable
import org.ooni.probe.data.models.SettingsCategoryItem

val DONATE_SETTINGS_ITEM: SettingsCategoryItem? get() = null

@Composable
fun DonateScreen(
    onBack: () -> Unit,
    openUrl: (String) -> Boolean,
) {
}

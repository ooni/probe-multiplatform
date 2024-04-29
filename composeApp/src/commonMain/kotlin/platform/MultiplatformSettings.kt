package platform

import com.russhwolf.settings.Settings

// This pattern is and related cross platform code is taken from:
// https://github.com/joelkanyi/FocusBloom/blob/develop/shared/src/commonMain/kotlin/com/joelkanyi/focusbloom/platform/MultiplatformSettingsWrapper.kt
expect class MultiplatformSettings {
    fun createSettings(): Settings
}
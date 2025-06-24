package org.ooni.probe.data.models

import okio.Path

sealed interface PlatformAction {
    data class Mail(
        val to: String,
        val subject: String,
        val body: String,
        val chooserTitle: String? = null,
        val attachment: Path? = null,
    ) : PlatformAction

    data class OpenUrl(val url: String) : PlatformAction

    data class Share(val text: String) : PlatformAction

    data class FileSharing(val title: String, val filePath: Path) : PlatformAction

    data object VpnSettings : PlatformAction

    data object LanguageSettings : PlatformAction
}

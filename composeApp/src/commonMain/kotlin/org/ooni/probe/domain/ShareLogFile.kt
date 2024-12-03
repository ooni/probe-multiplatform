package org.ooni.probe.domain

import okio.Path
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Logs
import org.jetbrains.compose.resources.getString
import org.ooni.probe.data.models.PlatformAction

class ShareLogFile(
    private val shareFile: (PlatformAction) -> Boolean,
    private val getAppLoggerFile: () -> Path,
) {
    suspend operator fun invoke(): Boolean =
        shareFile(
            PlatformAction.FileSharing(
                title = getString(Res.string.Settings_Logs),
                filePath = getAppLoggerFile(),
            ),
        )
}

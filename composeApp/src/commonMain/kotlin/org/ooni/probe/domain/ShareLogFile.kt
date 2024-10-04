package org.ooni.probe.domain

import okio.Path
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.logs
import org.jetbrains.compose.resources.getString
import org.ooni.probe.data.models.FileSharing

class ShareLogFile(
    private val shareFile: (FileSharing) -> Boolean,
    private val getAppLoggerFile: () -> Path,
) {
    suspend operator fun invoke(): Boolean =
        shareFile(
            FileSharing(
                title = getString(Res.string.logs),
                filePath = getAppLoggerFile(),
            ),
        )
}

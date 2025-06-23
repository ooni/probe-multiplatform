package org.ooni.probe.domain

import okio.Path
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Support_SendEmail
import ooniprobe.composeapp.generated.resources.shareEmailTo
import ooniprobe.composeapp.generated.resources.shareSubject
import org.jetbrains.compose.resources.getString
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.shared.PlatformInfo

class SendSupportEmail(
    private val platformInfo: PlatformInfo,
    private val launchAction: (PlatformAction) -> Boolean,
    private val getAppLoggerFile: suspend () -> Path,
) {
    suspend operator fun invoke(params: Params): Boolean {
        val supportEmail = getString(Res.string.shareEmailTo)
        val subject = getString(Res.string.shareSubject, platformInfo.version)
        val chooserTitle = getString(Res.string.Settings_Support_SendEmail)
        val body = params.text +
            "\n\n" +
            "PLATFORM: ${platformInfo.platform.name}\n" +
            (if (platformInfo.model.isNotBlank()) "MODEL: ${platformInfo.model}\n" else "") +
            "OS Version: ${platformInfo.osVersion}"

        return launchAction(
            PlatformAction.Mail(
                to = supportEmail,
                subject = subject,
                body = body,
                attachment = if (params.includeLogs) getAppLoggerFile() else null,
                chooserTitle = chooserTitle,
            ),
        )
    }

    data class Params(
        val text: String,
        val includeLogs: Boolean,
    )
}

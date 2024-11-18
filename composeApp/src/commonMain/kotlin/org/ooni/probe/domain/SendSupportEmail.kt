package org.ooni.probe.domain

import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_SendEmail_Label
import ooniprobe.composeapp.generated.resources.Settings_SendEmail_Message
import ooniprobe.composeapp.generated.resources.shareEmailTo
import ooniprobe.composeapp.generated.resources.shareSubject
import org.jetbrains.compose.resources.getString
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.shared.PlatformInfo

class SendSupportEmail(
    private val platformInfo: PlatformInfo,
    private val launchAction: (PlatformAction) -> Boolean,
) {
    suspend operator fun invoke() {
        getString(Res.string.shareEmailTo)
        val supportEmail = getString(Res.string.shareEmailTo)
        val subject = getString(Res.string.shareSubject, platformInfo.version)
        val chooserTitle = getString(Res.string.Settings_SendEmail_Label)
        val body = getString(Res.string.Settings_SendEmail_Message) + "\n\n\n" +
            "PLATFORM: ${platformInfo.platform}\n" +
            "MODEL: ${platformInfo.model}\n" +
            "OS Version: ${platformInfo.osVersion}"

        launchAction(
            PlatformAction.Mail(
                to = supportEmail,
                subject = subject,
                body = body,
                chooserTitle = chooserTitle,
            ),
        )
    }
}

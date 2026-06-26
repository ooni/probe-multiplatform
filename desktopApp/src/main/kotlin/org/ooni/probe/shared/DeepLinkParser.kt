package org.ooni.probe.shared

import co.touchlab.kermit.Logger
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.DeepLink
import java.net.URI

object DeepLinkParser {
    operator fun invoke(url: String): DeepLink {
        val uri = try {
            URI.create(url)
        } catch (e: Exception) {
            Logger.w("Invalid deep link: $url", e)
            return DeepLink.Error
        }

        return if (
            (uri.scheme == "ooni" && uri.host == "runv2") ||
            uri.host == OrganizationConfig.ooniRunDomain
        ) {
            uri.path.split("/").lastOrNull()?.let { id ->
                DeepLink.AddDescriptor(id)
            } ?: run {
                Logger.w("Invalid deep link: $uri")
                DeepLink.Error
            }
        } else if (uri.scheme == "http" || uri.scheme == "https") {
            DeepLink.RunUrls(url)
        } else {
            Logger.w("Invalid deep link: $uri")
            DeepLink.Error
        }
    }
}

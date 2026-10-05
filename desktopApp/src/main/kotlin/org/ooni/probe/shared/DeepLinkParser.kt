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

        val host = uri.host
        val path = uri.path ?: ""

        return if (
            host == "runv2" ||
            (host == OrganizationConfig.ooniRunDomain && (path.startsWith("/runv2") || path.startsWith("/v2")))
        ) {
            uri.path.split("/").lastOrNull()?.let { id ->
                DeepLink.AddDescriptor(id)
            } ?: run {
                Logger.w("Invalid deep link: $uri")
                DeepLink.Error
            }
        } else if (
            host == "login" ||
            (host == OrganizationConfig.ooniRunDomain && path.startsWith("/login"))
        ) {
            uri.query
                ?.split("&")
                ?.firstOrNull { it.startsWith("token=") }
                ?.substringAfter("token=")
                ?.takeIf { it.isNotBlank() }
                ?.let { token -> DeepLink.Login(token) }
                ?: run {
                    Logger.w("Login deep link without a token: $uri")
                    DeepLink.Error
                }
        } else if (host == OrganizationConfig.ooniRunDomain || uri.scheme == "http" || uri.scheme == "https") {
            DeepLink.RunUrls(url)
        } else {
            Logger.w("Invalid deep link: $uri")
            DeepLink.Error
        }
    }
}

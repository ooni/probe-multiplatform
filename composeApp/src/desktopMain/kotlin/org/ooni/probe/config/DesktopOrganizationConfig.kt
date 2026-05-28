package org.ooni.probe.config

import org.ooni.probe.DesktopBuildConfig

/**
 * Desktop-scoped wrapper around [OrganizationConfig] that appends `.dev` to
 * [appId] and [baseSoftwareName] in debug builds. Used to namespace OS-level
 * secure-storage identifiers (Keychain service / libsecret schema / Credential
 * Manager target prefix) so a dev run cannot read, overwrite, or invalidate the
 * entries a production install wrote on the same machine.
 *
 * Mirrors Android's `applicationIdSuffix = ".dev"`. All other fields delegate
 * to [OrganizationConfig] unchanged.
 */
object DesktopOrganizationConfig : OrganizationConfigInterface by OrganizationConfig {
    private const val DEV_SUFFIX = ".dev"

    override val appId: String =
        if (DesktopBuildConfig.IS_DEBUG) "${OrganizationConfig.appId}$DEV_SUFFIX" else OrganizationConfig.appId

    override val baseSoftwareName: String =
        if (DesktopBuildConfig.IS_DEBUG) "${OrganizationConfig.baseSoftwareName}$DEV_SUFFIX" else OrganizationConfig.baseSoftwareName
}

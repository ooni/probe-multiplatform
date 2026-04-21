package org.ooni.probe.shared

import org.ooni.probe.DesktopBuildConfig

/**
 * Runtime view of the desktop distribution channel. Mirrors the buildSrc
 * `Distribution` enum so runtime code can query capabilities through one
 * typed object instead of scattered boolean reads.
 *
 * The active channel is resolved from the generated
 * `DesktopBuildConfig.DISTRIBUTION` constant. Unknown values fall back to
 * [Direct] so a stale generated file can never disable self-updates
 * for a direct build.
 */
enum class Distribution {
    Direct,
    MacAppStore,
    MicrosoftStore,
    ;

    val supportsSelfUpdate: Boolean get() = this == Direct
    val requiresSandbox: Boolean get() = this == MacAppStore
    val isAppStore: Boolean get() = this != Direct

    val storeLandingUrl: String?
        get() = when (this) {
            MacAppStore -> null // TODO: wire once the app is listed
            MicrosoftStore -> null // TODO: wire once the app is listed
            Direct -> null
        }

    companion object {
        val current: Distribution by lazy {
            runCatching { valueOf(DesktopBuildConfig.DISTRIBUTION) }
                .getOrDefault(Direct)
        }
    }
}

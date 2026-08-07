package org.ooni.probe.config

/**
 * Non-UI configuration values probeCore's engine/domain layer needs, supplied by the hosting app.
 *
 * `composeApp` builds this from its flavor / build-type config (`OrganizationConfig`,
 * `BuildTypeDefaults`, generated `SharedBuildConfig`); the CLI supplies OONI defaults. This keeps
 * `probeCore` free of `composeApp`'s flavor source sets and generated build config.
 */
data class CoreConfig(
    val baseSoftwareName: String,
    val ooniApiBaseUrl: String,
    val passportVersion: String,
    /**
     * Overrides the platform-derived engine suffix in the reported software name
     * (`<baseSoftwareName>-<engineName>`). `null` keeps the host platform's name
     * (`android`/`ios`/`desktop`); the CLI sets it to `cli` so it reports `ooniprobe-cli`
     * instead of `ooniprobe-desktop`.
     */
    val engineName: String? = null,
)

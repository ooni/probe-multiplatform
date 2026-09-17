package org.ooni.probe.core

/**
 * CLI-facing autorun surface exposed by probeCore.
 *
 * It reports the current autorun readiness by reusing the canonical domain use cases
 * ([org.ooni.probe.domain.GetAutoRunSettings], [org.ooni.probe.domain.CheckAutoRunConstraints],
 * [org.ooni.probe.domain.GetAutoRunSpecification]) so `:cliApp` never re-implements the autorun
 * selection or constraint rules. It only reads shared state (preferences + stored results) and the
 * bundled descriptor set; it never starts a measurement or a scheduler.
 *
 * Service supervision (`autorun start`/`stop`) is intentionally NOT part of this surface: the CLI
 * layer reports it as an unsupported-on-this-platform error (see
 * `.omo/artifacts/cli-completion-full-parity/parity-blockers.md`).
 */
interface CliAutoRunGateway {
    suspend fun status(): CliAutoRunStatus

    fun close()
}

/**
 * UI-free snapshot of autorun readiness.
 *
 * - [enabled]: autorun is enabled/configured in preferences (onboarding + upload + automated-testing).
 * - [wifiOnly] / [onlyWhileCharging]: the configured autorun constraints (only meaningful when enabled).
 * - [constraintsSatisfied]: [org.ooni.probe.domain.CheckAutoRunConstraints] result for the current
 *   shared state (VPN/Wi-Fi/charging/not-uploaded-limit as applicable to this platform).
 * - [descriptorCount] / [testCount]: summary of the [org.ooni.probe.domain.GetAutoRunSpecification]
 *   spec (number of descriptor groups and total netTests that autorun would execute).
 */
data class CliAutoRunStatus(
    val enabled: Boolean,
    val wifiOnly: Boolean,
    val onlyWhileCharging: Boolean,
    val constraintsSatisfied: Boolean,
    val descriptorCount: Int,
    val testCount: Int,
)

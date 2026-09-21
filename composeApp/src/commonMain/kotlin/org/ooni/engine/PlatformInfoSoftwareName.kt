package org.ooni.engine

import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.shared.PlatformInfo

/**
 * Flavor-aware software-name helpers used by the UI app's check-in path.
 *
 * These live in `composeApp` because they read the flavor `OrganizationConfig.baseSoftwareName`.
 * `probeCore`'s `Engine` computes the same value from the injected `CoreConfig` instead.
 */
val PlatformInfo.softwareName
    get() = OrganizationConfig.baseSoftwareName + "-" + platform.engineName

fun PlatformInfo.buildSoftwareName(taskOrigin: TaskOrigin) =
    softwareName + (if (taskOrigin == TaskOrigin.AutoRun) "-" + "unattended" else "")

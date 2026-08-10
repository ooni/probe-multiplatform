package org.ooni.engine

import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.shared.Platform

/**
 * Software name reported to the OONI backend, e.g. `ooniprobe-desktop` (with a `-unattended`
 * suffix for auto-run). [baseSoftwareName] comes from the injected `CoreConfig` so probeCore stays
 * independent of composeApp's flavor config.
 */
fun buildSoftwareName(
    baseSoftwareName: String,
    platform: Platform,
    taskOrigin: TaskOrigin,
    engineName: String? = null,
): String {
    val base = "$baseSoftwareName-${engineName ?: platform.engineName}"
    return base + if (taskOrigin == TaskOrigin.AutoRun) "-unattended" else ""
}

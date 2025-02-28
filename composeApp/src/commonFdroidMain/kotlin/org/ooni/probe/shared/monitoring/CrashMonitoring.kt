package org.ooni.probe.shared.monitoring

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.shared.PlatformInfo

class CrashMonitoring(
    private val preferencesRepository: PreferenceRepository,
    private val platformInfo: PlatformInfo,
) {
    private var isEnabled = false

    suspend fun setup() {}

    val logWriter = object : LogWriter() {
        override fun isLoggable(
            tag: String,
            severity: Severity,
        ): Boolean = isEnabled

        override fun log(
            severity: Severity,
            message: String,
            tag: String,
            throwable: Throwable?,
        ) {
        }

        private fun addBreadcrumb(
            severity: Severity,
            message: String,
            tag: String,
        ) {
        }
    }
}

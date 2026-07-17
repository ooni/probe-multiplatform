package org.ooni.probe.shared.monitoring

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.ooni.engine.softwareName
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.shared.PlatformInfo

class CrashMonitoring(
    private val preferencesRepository: PreferenceRepository,
    private val platformInfo: PlatformInfo,
) {
    /**
     * Sentry's canonical release format. Both organizations report to the same project per
     * platform, and share a version, so the package is the only thing separating them: Sentry
     * parses it out of this format, which is what makes per-organization release health work.
     * Not [PlatformInfo.version], which is user-facing.
     */
    private val release
        get() = "${OrganizationConfig.appId}@${platformInfo.buildName}+${platformInfo.buildNumber}"

    suspend fun setup() {
        preferencesRepository
            .getValueByKey(SettingsKey.SEND_CRASH)
            .onEach { sendCrash ->
                if (sendCrash == true) {
                    Sentry.init {
                        it.dsn = platformInfo.sentryDsn
                        it.release = release
                        it.tracesSampleRate = 1.0
                        platformInfo.sentryExtraTags["environment"]?.let { environment ->
                            it.environment = environment
                        }
                    }
                    Sentry.configureScope { scope ->
                        scope.setTag("software_name", platformInfo.softwareName)
                        platformInfo.sentryExtraTags.forEach { (key, value) ->
                            scope.setTag(key, value)
                        }
                    }
                } else {
                    Sentry.close()
                }
            }.collect()
    }

    val logWriter = object : LogWriter() {
        override fun isLoggable(
            tag: String,
            severity: Severity,
        ): Boolean = Sentry.isEnabled() && severity != Severity.Verbose

        override fun log(
            severity: Severity,
            message: String,
            tag: String,
            throwable: Throwable?,
        ) {
            if (!Sentry.isEnabled()) return

            if (
                (severity == Severity.Warn || severity == Severity.Error) &&
                (throwable != null || MESSAGES_TO_SKIP_REPORT.none { message.contains(it) })
            ) {
                if (throwable != null) {
                    if (message.isNotBlank()) {
                        addBreadcrumb(severity, message, tag)
                    }
                    Sentry.captureException(throwable)
                } else {
                    Sentry.captureMessage(message)
                }
            } else {
                addBreadcrumb(severity, message, tag)
            }
        }

        private fun addBreadcrumb(
            severity: Severity,
            message: String,
            tag: String,
        ) {
            Sentry.addBreadcrumb(
                Breadcrumb(
                    level = when (severity) {
                        Severity.Verbose,
                        Severity.Debug,
                        -> SentryLevel.DEBUG

                        Severity.Info -> SentryLevel.INFO
                        Severity.Warn -> SentryLevel.WARNING
                        Severity.Error -> SentryLevel.ERROR
                        Severity.Assert -> SentryLevel.ERROR
                    },
                    type = when (severity) {
                        Severity.Debug -> "debug"
                        Severity.Error -> "error"
                        else -> "default"
                    },
                    message = message,
                    category = tag,
                ),
            )
        }
    }

    companion object {
        private val MESSAGES_TO_SKIP_REPORT = listOf(
            "Picking from default OpenVPN endpoints",
            "sessionresolver: LookupHost failed",
            "We disabled the",
            "experiment not enabled by check-in API",
        )
    }
}

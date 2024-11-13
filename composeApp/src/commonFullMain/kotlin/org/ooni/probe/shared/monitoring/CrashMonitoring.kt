package org.ooni.probe.shared.monitoring

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository

class CrashMonitoring(
    private val preferencesRepository: PreferenceRepository,
) {
    suspend fun setup() {
        preferencesRepository.getValueByKey(SettingsKey.SEND_CRASH)
            .onEach { sendCrash ->
                if (sendCrash == true) {
                    Sentry.init {
                        it.dsn = SENTRY_DSN
                    }
                } else {
                    Sentry.close()
                }
            }
            .collect()
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

            if (severity == Severity.Warn || severity == Severity.Error) {
                if (throwable != null) {
                    addBreadcrumb(severity, message, tag)
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
        private const val SENTRY_DSN =
            "https://9dcd83d9519844188803aa817cdcd416@o155150.ingest.sentry.io/5619989"
    }
}

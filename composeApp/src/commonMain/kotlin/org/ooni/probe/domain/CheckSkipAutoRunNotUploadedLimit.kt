package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.probe.data.models.SettingsKey

class CheckSkipAutoRunNotUploadedLimit(
    private val countResultsMissingUpload: () -> Flow<Long>,
    private val getPreferenceValueByKey: (SettingsKey) -> Flow<Any?>,
) {
    suspend operator fun invoke(): Boolean {
        val limitPreference =
            getPreferenceValueByKey(SettingsKey.AUTOMATED_TESTING_NOT_UPLOADED_LIMIT).first()
        val count = countResultsMissingUpload().first()

        val notUploadedLimit = (limitPreference as? Int)
            ?.coerceAtLeast(1)
            ?: BootstrapPreferences.NOT_UPLOADED_LIMIT_DEFAULT
        val shouldSkip = count >= notUploadedLimit

        if (shouldSkip) {
            Logger.w(
                "Skipping auto-run due to not uploaded limit",
                SkipAutoRunException("Results missing upload: $count (limit=$notUploadedLimit)"),
            )
        }

        return shouldSkip
    }
}

class SkipAutoRunException(message: String) : Exception(message)

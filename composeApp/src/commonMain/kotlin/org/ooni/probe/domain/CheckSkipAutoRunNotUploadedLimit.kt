package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.ooni.probe.data.models.SettingsKey

class CheckSkipAutoRunNotUploadedLimit(
    private val countResultsMissingUpload: () -> Flow<Long>,
    private val getPreferenceValueByKey: (SettingsKey) -> Flow<Any?>,
) {
    operator fun invoke(): Flow<Boolean> =
        combine(
            getPreferenceValueByKey(SettingsKey.AUTOMATED_TESTING_NOT_UPLOADED_LIMIT),
            countResultsMissingUpload(),
        ) { limit, count ->
            val notUploadedLimit = (limit as? Int)
                ?.coerceAtLeast(1)
                ?: BootstrapPreferences.NOT_UPLOADED_LIMIT_DEFAULT
            count >= notUploadedLimit
        }
}

package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CheckSkipAutoRunNotUploadedLimit(
    private val countResultsMissingUpload: () -> Flow<Long>,
) {
    suspend operator fun invoke(): Boolean {
        val count = countResultsMissingUpload().first()
        val shouldSkip = count >= NOT_UPLOADED_LIMIT
        if (shouldSkip) {
            Logger.w(
                "Skipping auto-run due to not uploaded limit",
                SkipAutoRunException("Results missing upload: $count (limit=$NOT_UPLOADED_LIMIT)"),
            )
        }
        return shouldSkip
    }

    companion object {
        private const val NOT_UPLOADED_LIMIT = 10
    }
}

class SkipAutoRunException(message: String) : Exception(message)

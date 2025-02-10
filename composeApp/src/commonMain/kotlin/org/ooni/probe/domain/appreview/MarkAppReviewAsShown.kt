package org.ooni.probe.domain.appreview

import kotlinx.datetime.LocalDateTime
import org.ooni.probe.shared.now

class MarkAppReviewAsShown(
    private val setShownAt: suspend (LocalDateTime) -> Unit,
) {
    suspend operator fun invoke() {
        setShownAt(LocalDateTime.now())
    }
}

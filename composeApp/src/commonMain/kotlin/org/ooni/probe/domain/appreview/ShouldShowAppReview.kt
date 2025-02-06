package org.ooni.probe.domain.appreview

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import org.ooni.probe.shared.now
import org.ooni.probe.shared.today

class ShouldShowAppReview(
    private val incrementLaunchTimes: suspend () -> Unit,
    private val getLaunchTimes: suspend () -> Long,
    private val getShownAt: suspend () -> LocalDateTime?,
    private val getFirstOpenAt: suspend () -> LocalDateTime?,
    private val setFirstOpenAt: suspend (LocalDateTime) -> Unit,
) {
    suspend operator fun invoke(): Boolean {
        incrementLaunchTimes()
        if (getShownAt() != null) return false

        val firstOpenAt = getFirstOpenAt()
            ?: LocalDateTime.now().also { setFirstOpenAt(it) }
        val launchTimes = getLaunchTimes()

        return launchTimes >= SHOW_AFTER_LAUNCH_TIMES &&
            (firstOpenAt.date.daysUntil(LocalDate.today()) >= SHOW_AFTER_FIRST_OPEN_DAYS)
    }

    companion object {
        private const val SHOW_AFTER_LAUNCH_TIMES = 5
        private const val SHOW_AFTER_FIRST_OPEN_DAYS = 3
    }
}

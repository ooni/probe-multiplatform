package org.ooni.probe.domain.credentials

import org.ooni.passport.models.ParamRange
import org.ooni.probe.data.models.Manifest

class ResolveSubmissionPolicy {
    data class Ranges(
        val ageRange: ParamRange,
        val measurementCountRange: ParamRange,
    )

    operator fun invoke(
        manifest: Manifest,
        probeCc: String,
        probeAsn: String,
    ): Ranges? {
        for (entry in manifest.manifest.submissionPolicy) {
            val ccOk = entry.match.probeCc == WILDCARD || entry.match.probeCc == probeCc
            val asnOk = entry.match.probeAsn == WILDCARD || entry.match.probeAsn == probeAsn
            if (ccOk && asnOk) {
                val age = entry.policy.age.toRange() ?: continue
                val count = entry.policy.measurementCount.toRange() ?: continue
                return Ranges(age, count)
            }
        }
        return null
    }

    private fun List<UInt>.toRange(): ParamRange? = if (size >= 2) ParamRange(min = this[0], max = this[1]) else null

    companion object {
        private const val WILDCARD = "*"
    }
}

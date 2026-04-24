package org.ooni.passport.models

enum class VerificationStatus {
    Verified,
    Failed,
    Unverified,
    Unknown,
    ;

    companion object {
        fun fromWire(value: String?): VerificationStatus =
            when (value) {
                "verified" -> Verified
                "failed" -> Failed
                "unverified" -> Unverified
                else -> Unknown
            }
    }
}

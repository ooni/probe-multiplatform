package org.ooni.passport.models

enum class VerificationStatus {
    Verified,
    Failed,
    Unverified,
    Unknown,
    ;

    companion object {
        fun fromPassport(value: String?): VerificationStatus =
            when (value) {
                "verified" -> Verified
                "failed" -> Failed
                "unverified" -> Unverified
                else -> Unknown
            }
    }
}

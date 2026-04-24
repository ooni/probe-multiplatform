package org.ooni.passport.models

data class SubmitCredentialConfig(
    val credential: String,
    val publicParams: String,
    val manifestVersion: String,
    val ageRange: ParamRange,
    val measurementCountRange: ParamRange,
)

package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.SubmitCredentialConfig
import org.ooni.passport.models.SubmitError
import org.ooni.passport.models.VerificationStatus
import org.ooni.probe.config.BuildTypeDefaults
import org.ooni.probe.data.models.Credential
import org.ooni.probe.data.models.Manifest
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.domain.SubmitMeasurement
import org.ooni.probe.shared.monitoring.Instrumentation
import org.ooni.probe.shared.monitoring.reportTransaction

class SubmitMeasurementWithUser(
    private val getManifest: () -> Flow<Manifest?>,
    private val getCredential: suspend () -> Credential?,
    private val setCredential: SetCredential,
    private val stampMeasurement: StampMeasurement,
    private val resolveSubmissionPolicy: ResolveSubmissionPolicy,
    private val userAuthSubmit: suspend (
        url: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        credentialConfig: SubmitCredentialConfig?,
    ) -> Result<CredentialResponse, PassportException>,
    private val json: Json,
    private val handleSubmitOutcome: suspend (VerificationStatus, SubmitError?) -> Unit = { _, _ -> },
) {
    suspend operator fun invoke(measurementData: String): Result<SubmitMeasurement.ResponseData, Throwable?> {
        val manifest = getManifest().first() ?: return Failure(null)
        val credential = getCredential()
        val stamped = stampMeasurement(measurementData)
        val data = parseMeasurementData(stamped) ?: return Failure(null)

        val credentialConfig = buildCredentialConfig(manifest, credential, data)

        return when (
            val result = userAuthSubmit(
                "${BuildTypeDefaults.ooniApiBaseUrl}/api/v1/submit_measurement",
                stamped,
                data.probeCc,
                data.probeAsn,
                credentialConfig,
            )
        ) {
            is Failure -> {
                Logger.w("Failed to submit measurement with user", result.reason)
                Failure(result.reason)
            }

            is Success -> {
                val credentialResponse = result.value
                if (!credentialResponse.response.isSuccessful) {
                    val exception = PassportException.HttpRequestUnsuccessful(
                        statusCode = credentialResponse.response.statusCode,
                        responseBody = credentialResponse.response.bodyText?.take(MAX_ERROR_BODY_LENGTH),
                    )
                    Logger.w("Submit returned non-2XX", exception)
                    Instrumentation.reportTransaction(
                        operation = "SubmitHttpError",
                        data = mapOf(
                            "status_code" to credentialResponse.response.statusCode,
                            "error" to (credentialResponse.response.bodyText?.take(10) ?: "unknown"),
                        ),
                    )
                    httpRecoveryError(credentialResponse.response.statusCode)?.let { recoveryError ->
                        handleSubmitOutcome(VerificationStatus.Unknown, recoveryError)
                    }
                    Failure(exception)
                } else {
                    val outcome = credentialResponse.decodeSubmitOutcome(json)
                    val submitBody = credentialResponse.response.bodyText?.let(this::parseResponse)

                    if (outcome.verificationStatus == VerificationStatus.Verified) {
                        if (credential is Credential && credentialResponse.credential is String) {
                            setCredential(
                                credential.copy(
                                    credential = credentialResponse.credential,
                                ),
                            )
                        }
                    }

                    Success(
                        SubmitMeasurement.ResponseData(
                            uid = submitBody?.measurementUid?.ifBlank { null }?.let(MeasurementModel::Uid),
                            verificationStatus = outcome.verificationStatus,
                            submitError = outcome.error,
                        ),
                    )
                }
            }
        }
    }

    private fun httpRecoveryError(statusCode: Int): SubmitError? =
        when (statusCode) {
            HTTP_UNAUTHORIZED,
            HTTP_FORBIDDEN,
            -> SubmitError.CredentialError
            HTTP_NOT_FOUND -> SubmitError.ManifestNotFound
            else -> null
        }

    private suspend fun buildCredentialConfig(
        manifest: Manifest,
        credential: Credential?,
        data: MeasurementData,
    ): SubmitCredentialConfig? {
        if (credential == null) return null
        val ranges = resolveSubmissionPolicy(manifest, data.probeCc, data.probeAsn) ?: run {
            // We have a credential but the manifest has no submission_policy entry for this
            // cc/asn, so we can't derive sanctioned param ranges and submit unauthenticated.
            // Report it so the policy-coverage gap is visible in aggregate.
            Logger.w("No submission_policy entry for cc=${data.probeCc} asn=${data.probeAsn}")
            Instrumentation.reportTransaction(
                operation = "SubmitMissingPolicy",
                data = mapOf(
                    "probe_cc" to data.probeCc,
                    "probe_asn" to data.probeAsn,
                ),
            )
            return null
        }
        return SubmitCredentialConfig(
            credential = credential.credential,
            publicParams = manifest.manifest.publicParameters,
            manifestVersion = manifest.meta.version,
            ageRange = ranges.ageRange,
            measurementCountRange = ranges.measurementCountRange,
        )
    }

    private fun parseMeasurementData(measurementData: String): MeasurementData? =
        try {
            json.decodeFromString<MeasurementData>(measurementData)
        } catch (e: Exception) {
            Logger.w("Could not parse measurement data", e)
            null
        }

    private fun parseResponse(response: String): SubmitResponse? =
        try {
            json.decodeFromString<SubmitResponse>(response)
        } catch (e: Exception) {
            Logger.w("Could not parse submit response", e)
            null
        }

    @Serializable
    private data class MeasurementData(
        @SerialName("probe_cc") val probeCc: String,
        @SerialName("probe_asn") val probeAsn: String,
    )

    @Serializable
    private data class SubmitResponse(
        @SerialName("measurement_uid") val measurementUid: String? = null,
    )

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
        private const val MAX_ERROR_BODY_LENGTH = 500
    }
}

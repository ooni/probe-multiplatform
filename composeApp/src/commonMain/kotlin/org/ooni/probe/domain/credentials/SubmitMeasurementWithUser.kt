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
import org.ooni.passport.PassportAuthSubmit
import org.ooni.passport.models.SubmitCredentialConfig
import org.ooni.passport.models.VerificationStatus
import org.ooni.probe.config.BuildTypeDefaults
import org.ooni.probe.data.models.Credential
import org.ooni.probe.data.models.Manifest
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.domain.SubmitMeasurement

class SubmitMeasurementWithUser(
    private val getManifest: () -> Flow<Manifest?>,
    private val getCredential: suspend () -> Credential?,
    private val setCredential: SetCredential,
    private val stampMeasurement: StampMeasurement,
    private val resolveSubmissionPolicy: ResolveSubmissionPolicy,
    private val passportAuthSubmit: PassportAuthSubmit,
    private val json: Json,
) {
    suspend operator fun invoke(
        measurementData: String,
        reportId: MeasurementModel.ReportId,
    ): Result<SubmitMeasurement.ResponseData, Throwable?> {
        val manifest = getManifest().first() ?: return Failure(null)
        val credential = getCredential()
        val stamped = stampMeasurement(measurementData)
        val data = parseMeasurementData(stamped) ?: return Failure(null)

        val credentialConfig = buildCredentialConfig(manifest, credential, data)

        passportAuthSubmit
            .userAuthSubmit(
                url = "${BuildTypeDefaults.ooniApiBaseUrl}/api/v1/submit_measurement/$reportId",
                content = stamped,
                probeCc = data.probeCc,
                probeAsn = data.probeAsn,
                credentialConfig = credentialConfig,
            ).onSuccess { result ->
                if (!result.response.isSuccessful) {
                    Logger.w("Submit returned non-2XX: ${result.response.statusCode}")
                    return Failure(null)
                }

                val outcome = result.decodeSubmitOutcome(json)
                val submitBody = result.response.bodyText?.let(this::parseResponse)

                if (outcome.verificationStatus == VerificationStatus.Verified) {
                    result.decodeCredential(json)?.let { setCredential(it) }
                }

                return Success(
                    SubmitMeasurement.ResponseData(
                        uid = submitBody?.measurementUid?.let(MeasurementModel::Uid),
                        verificationStatus = outcome.verificationStatus,
                        submitError = outcome.error,
                    ),
                )
            }.onFailure {
                Logger.w("Failed to submit measurement with user", it)
                return Failure(it)
            }

        return Failure(null)
    }

    private fun buildCredentialConfig(
        manifest: Manifest,
        credential: Credential?,
        data: MeasurementData,
    ): SubmitCredentialConfig? {
        if (credential == null) return null
        val ranges = resolveSubmissionPolicy(manifest, data.probeCc, data.probeAsn) ?: run {
            Logger.w("No submission_policy entry for cc=${data.probeCc} asn=${data.probeAsn}")
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
}

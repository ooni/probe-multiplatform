import composeApp

class IosPassportBridge: PassportBridge {
    func get(
        url: String,
        headers: [PassportBridgeKeyValue],
        query: [PassportBridgeKeyValue]
    ) -> Result<PassportHttpResponse, PassportException> {
        do {
            let response = try clientGet(
                url: url,
                headers: headers.map { KeyValue(key: $0.key, value: $0.value) },
                query: query.map { KeyValue(key: $0.key, value: $0.value) }
            )
            return IosPassportBridgeHelpersKt.SuccessPassportHttpResponse(value: response.toPassport())
        } catch let error as OoniError {
            return IosPassportBridgeHelpersKt.FailureHttpPassportException(reason: error.toPassport())
        } catch {
            return IosPassportBridgeHelpersKt.FailureHttpPassportException(
                reason: PassportException.Other(message: error.localizedDescription)
            )
        }
    }

    func post(url: String, headers: [PassportBridgeKeyValue], payload: String) -> Result<PassportHttpResponse, PassportException> {
        do {
            let response = try clientPost(
                url: url,
                headers: headers.map { KeyValue(key: $0.key, value: $0.value) },
                payload: payload
            )
            return IosPassportBridgeHelpersKt.SuccessPassportHttpResponse(value: response.toPassport())
        } catch let error as OoniError {
            return IosPassportBridgeHelpersKt.FailureHttpPassportException(reason: error.toPassport())
        } catch {
            return IosPassportBridgeHelpersKt.FailureHttpPassportException(
                reason: PassportException.Other(message: error.localizedDescription)
            )
        }
    }

    func userAuthRegister(
        url: String,
        publicParams: String,
        manifestVersion: String
    ) -> Result<CredentialResponse, PassportException> {
        do {
            let response = try userauthRegister(
                url: url,
                publicParams: publicParams,
                manifestVersion: manifestVersion
            )
            return IosPassportBridgeHelpersKt.SuccessCredentialResponse(value: response.toPassport())
        } catch let error as OoniError {
            return IosPassportBridgeHelpersKt.FailureCredentialPassportException(reason: error.toPassport())
        } catch {
            return IosPassportBridgeHelpersKt.FailureCredentialPassportException(
                reason: PassportException.Other(message: error.localizedDescription)
            )
        }
    }

    func userAuthSubmit(
        url: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        credentialConfig: SubmitCredentialConfig?
    ) -> Result<CredentialResponse, PassportException> {
        do {
            let response = try userauthSubmit(
                url: url,
                content: content,
                probeCc: probeCc,
                probeAsn: probeAsn,
                credentialConfig: credentialConfig?.toUniffi()
            )
            return IosPassportBridgeHelpersKt.SuccessCredentialResponse(value: response.toPassport())
        } catch let error as OoniError {
            return IosPassportBridgeHelpersKt.FailureCredentialPassportException(reason: error.toPassport())
        } catch {
            return IosPassportBridgeHelpersKt.FailureCredentialPassportException(
                reason: PassportException.Other(message: error.localizedDescription)
            )
        }
    }

    func getProbeId(
        credentialB64: String,
        probeAsn: String,
        probeCc: String
    ) -> Result<NSString, PassportException> {
        do {
            let result = try passportGetProbeId(
                credentialB64: credentialB64,
                probeAsn: probeAsn,
                probeCc: probeCc
            )
            return IosPassportBridgeHelpersKt.SuccessProbeId(value: result.probeId)
        } catch let error as OoniError {
            return IosPassportBridgeHelpersKt.FailureProbeIdPassportException(reason: error.toPassport())
        } catch {
            return IosPassportBridgeHelpersKt.FailureProbeIdPassportException(
                reason: PassportException.Other(message: error.localizedDescription)
            )
        }
    }
}

private func passportGetProbeId(
    credentialB64: String,
    probeAsn: String,
    probeCc: String
) throws -> ProbeIdResult {
    return try getProbeId(
        credentialB64: credentialB64,
        probeAsn: probeAsn,
        probeCc: probeCc
    )
}

extension HttpResponse {
    func toPassport() -> PassportHttpResponse {
        return PassportHttpResponse(
            statusCode: statusCode,
            version: version,
            headersListText: headersListText,
            bodyText: bodyText
        )
    }
}

extension CredentialResult {
    func toPassport() -> CredentialResponse {
        return CredentialResponse(
            response: response.toPassport(),
            credential: credential
        )
    }
}

extension SubmitCredentialConfig {
    func toUniffi() -> CredentialConfig {
        return CredentialConfig(
            credential: credential,
            publicParams: publicParams,
            manifestVersion: manifestVersion,
            ageRange: ParamRange(
                min: ageRange.min,
                max: ageRange.max
            ),
            measurementCountRange: ParamRange(
                min: measurementCountRange.min,
                max: measurementCountRange.max
            )
        )
    }
}

extension OoniError {
    func toPassport() -> PassportException {
        switch self {
        case .NullOrInvalidInput(let message):
            return PassportException.NullOrInvalidInput(message: message)
        case .Base64DecodeError(let message):
            return PassportException.Base64DecodeError(message: message)
        case .BinaryDecodeError(let message):
            return PassportException.BinaryDecodeError(message: message)
        case .HttpClientError(let message):
            return PassportException.HttpClientError(message: message)
        case .CryptoError(let message):
            return PassportException.CryptoError(message: message)
        case .SerializationError(let message):
            return PassportException.SerializationError(message: message)
        case .InvalidCredential(let message):
            return PassportException.InvalidCredential(message: message)
        case .Other(let message):
            return PassportException.Other(message: message)
        }
    }
}

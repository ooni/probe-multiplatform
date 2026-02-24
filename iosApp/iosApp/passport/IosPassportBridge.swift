import composeApp

class IosNativePassportBridge: NativePassportBridge {
    func nativeClientGet(url: String, headers: [KeyValuePair], query: [KeyValuePair]) -> NativeHttpResult {
        do {
            let response = try clientGet(
                url: url,
                headers: headers.map { KeyValue(key: $0.key, value: $0.value) },
                query: query.map { KeyValue(key: $0.key, value: $0.value) }
            )
            return NativeHttpResult.Success(response: response.toNative())
        } catch let error as OoniError {
            return NativeHttpResult.Error(exception: error.toPassport())
        } catch {
            return NativeHttpResult.Error(exception: PassportException.Other(message: error.localizedDescription))
        }
    }

    func nativeUserAuthRegister(url: String, publicParams: String, manifestVersion: String) -> NativeCredentialHttpResult {
        do {
            let response = try userauthRegister(
                url: url,
                publicParams: publicParams,
                manifestVersion: manifestVersion
            )
            return NativeCredentialHttpResult.Success(result: response.toNative())
        } catch let error as OoniError {
            return NativeCredentialHttpResult.Error(exception: error.toPassport())
        } catch {
            return NativeCredentialHttpResult.Error(exception: PassportException.Other(message: error.localizedDescription))
        }
    }

    func nativeUserAuthSubmit(url: String, credential: String, publicParams: String, content: String, probeCc: String, probeAsn: String, manifestVersion: String) -> NativeCredentialHttpResult {
        do {
            let response = try userauthSubmit(
                url: url,
                credential: credential,
                publicParams: publicParams,
                content: content,
                probeCc: probeCc,
                probeAsn: probeAsn,
                manifestVersion: manifestVersion
            )
            return NativeCredentialHttpResult.Success(result: response.toNative())
        } catch let error as OoniError {
            return NativeCredentialHttpResult.Error(exception: error.toPassport())
        } catch {
            return NativeCredentialHttpResult.Error(exception: PassportException.Other(message: error.localizedDescription))
        }
    }
}

extension HttpResponse {
    func toNative() -> NativeHttpResponse {
        return NativeHttpResponse(
            statusCode: statusCode,
            version: version,
            headersListText: headersListText,
            bodyText: bodyText
        )
    }
}

extension CredentialResult {
    func toNative() -> NativeCredentialResult {
        return NativeCredentialResult(
            response: response.toNative(),
            credential: credential
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

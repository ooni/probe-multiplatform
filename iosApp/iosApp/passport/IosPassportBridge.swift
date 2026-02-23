import composeApp
import uniffi.ooniprobe.HttpResponse
import uniffi.ooniprobe.KeyValue
import uniffi.ooniprobe.OoniException

class IosPassportBridge: PassportBridge {

    func clientGet(
        url: String,
        headers: [Map.Entry<String, String>],
        query: [Map.Entry<String, String>]
    ) -> Result<PassportHttpResponse, PassportException> {
        do {
            let response = try uniffi.ooniprobe.clientGet(
                url: url,
                headers: headers.map { KeyValue(it.key, it.value) },
                query: query.map { KeyValue(it.key, it.value) }
            )
            return Success(response.toPassport())
        } catch OoniException {
            return Failure(error.toPassport())
        }
    }

    func userAuthRegister(
        url: String,
        publicParams: String,
        manifestVersion: String
    ) -> Result<CredentialResult, PassportException> {
        do {
            let result = try uniffi.ooniprobe.userauthRegister(
                url: url,
                publicParams: publicParams,
                manifestVersion: manifestVersion
            )
            return Success(result.toPassport())
        } catch OoniException {
            return Failure(error.toPassport())
        }
    }

    func userAuthSubmit(
        url: String,
        credential: String,
        publicParams: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        manifestVersion: String
    ) -> Result<CredentialResult, PassportException> {
        do {
            let result = try uniffi.ooniprobe.userauthSubmit(
                url: url,
                credential: credential,
                publicParams: publicParams,
                content: content,
                probeCc: probeCc,
                probeAsn: probeAsn,
                manifestVersion: manifestVersion,
            )
            return Success(result.toPassport())
        } catch OoniException {
            return Failure(e.toPassport())
        }
    }
}

extension HttpResponse {
    func toPassport() -> PassportHttpResponse {
        return PassportHttpResponse(
            statusCode,
            version,
            headersListText,
            bodyText
        )
    }
}

extension uniffi.ooniprobe.CredentialResult {
    func toPassport() -> CredentialResult {
        return CredentialResult(
            response.toPassport(),
            credential
        )
    }
}

extension OoniException {
    func toPassport() -> PassportException {
        // TODO
    }
}

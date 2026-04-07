package org.ooni.passport.models

sealed class PassportException(
    message: String?,
) : Exception(message) {
    class NullOrInvalidInput(
        message: String?,
    ) : PassportException(message)

    class Base64DecodeError(
        message: String?,
    ) : PassportException(message)

    class BinaryDecodeError(
        message: String?,
    ) : PassportException(message)

    class HttpClientError(
        message: String?,
    ) : PassportException(message)

    class CryptoError(
        message: String?,
    ) : PassportException(message)

    class SerializationError(
        message: String?,
    ) : PassportException(message)

    class InvalidCredential(
        message: String?,
    ) : PassportException(message)

    class Other(
        message: String?,
    ) : PassportException(message)
}

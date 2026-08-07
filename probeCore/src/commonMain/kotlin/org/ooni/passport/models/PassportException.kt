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

    /**
     * The call was never dispatched because the device reported no usable network.
     *
     * This is the only failure the HTTP layer can *prove* is caused by being offline, so it's
     * the only one [isOfflineFailure] treats as expected. Everything else - including transport
     * and DNS errors, which the Rust bridge also reports as [HttpClientError] - stays diagnostic.
     */
    class Offline(
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

package org.ooni.engine

/**
 * Thrown by [SecureStorage.read] when storage is temporarily inaccessible
 * (e.g., iOS keychain locked after reboot), so callers do not mistake it for a missing value.
 */
class SecureStorageUnavailableException(
    key: String,
) : Exception("Secure storage temporarily unavailable for key: $key")

sealed interface DeleteResult {
    data class Deleted(
        val key: String,
    ) : DeleteResult

    data class NotFound(
        val key: String,
    ) : DeleteResult

    data class Error(
        val key: String,
        val message: String?,
        val cause: Throwable? = null,
    ) : DeleteResult
}

sealed interface WriteResult {
    data class Created(
        val key: String,
    ) : WriteResult

    data class Updated(
        val key: String,
    ) : WriteResult

    data class Error(
        val key: String,
        val message: String?,
        val cause: Throwable? = null,
    ) : WriteResult

    /**
     * Indicates storage is temporarily inaccessible (e.g., iOS keychain locked after reboot).
     * Callers should defer the operation instead of treating it as a failure.
     */
    data class TemporarilyUnavailable(
        val key: String,
    ) : WriteResult
}

sealed interface DeleteAllResult {
    data class DeletedCount(
        val count: Int,
    ) : DeleteAllResult

    data class Error(
        val message: String?,
        val cause: Throwable? = null,
    ) : DeleteAllResult
}

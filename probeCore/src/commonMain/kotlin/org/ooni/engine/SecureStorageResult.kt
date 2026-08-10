package org.ooni.engine

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

package org.ooni.probe.update

import org.ooni.probe.shared.UpdateError

/**
 * How a single update check against one appcast feed ended.
 */
sealed interface CheckOutcome {
    data object UpdateAvailable : CheckOutcome

    data object NoUpdate : CheckOutcome

    /** The feed (or the host serving the update) could not be reached, another feed may work. */
    data object FeedFailure : CheckOutcome

    /** Configuration, signature or installation problem that another feed would not fix. */
    data object OtherError : CheckOutcome

    val feedWorked: Boolean get() = this == UpdateAvailable || this == NoUpdate

    companion object {
        // Codes returned by the native bridge (SparkeBridge.m / WinSparkleBridge.c)
        private const val NETWORK_ERROR = -1 // also WinSparkle's code for any check failure
        private const val BRIDGE_EXCEPTION = -2
        private const val INVALID_PUBLIC_KEY = -3
        private const val INVALID_PUBLIC_KEY_LENGTH = -4
        private const val INIT_EXCEPTION = -5
        private const val SETUP_ERROR = -999

        // Sparkle error codes (SUErrors.h) for a feed or download that could not be fetched
        private const val SU_APPCAST_PARSE_ERROR = 1000
        private const val SU_APPCAST_ERROR = 1002
        private const val SU_DOWNLOAD_ERROR = 2001

        fun fromError(error: UpdateError): CheckOutcome =
            when (error.code) {
                NETWORK_ERROR,
                SU_APPCAST_PARSE_ERROR,
                SU_APPCAST_ERROR,
                SU_DOWNLOAD_ERROR,
                -> FeedFailure

                BRIDGE_EXCEPTION,
                INVALID_PUBLIC_KEY,
                INVALID_PUBLIC_KEY_LENGTH,
                INIT_EXCEPTION,
                SETUP_ERROR,
                -> OtherError

                // Other negative codes are NSURLError codes (connection, DNS, TLS, timeout)
                else -> if (error.code < 0) FeedFailure else OtherError
            }

        /** Short explanation of [UpdateError.code] for logs. */
        fun describe(error: UpdateError): String =
            when (error.code) {
                NETWORK_ERROR -> "network or URL error"
                BRIDGE_EXCEPTION -> "native updater exception"
                INVALID_PUBLIC_KEY, INVALID_PUBLIC_KEY_LENGTH ->
                    "EdDSA key validation failed, check the SPARKLE_PUBLIC_KEY configuration"
                INIT_EXCEPTION -> "exception during initialization"
                SETUP_ERROR -> "update system setup or validation error"
                SU_APPCAST_PARSE_ERROR -> "appcast could not be parsed"
                SU_APPCAST_ERROR -> "appcast error"
                SU_DOWNLOAD_ERROR -> "appcast or update could not be downloaded"
                else -> if (error.code < 0) "connection error" else "update error"
            }
    }
}

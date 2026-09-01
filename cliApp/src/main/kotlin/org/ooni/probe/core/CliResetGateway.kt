package org.ooni.probe.core

/**
 * CLI-facing reset surface: clears the OONI-scoped platform secure storage as part of
 * `reset --force`.
 *
 * The CLI drives the reset ordering ((1) close DB/core resources, (2) clear scoped secure storage,
 * (3) delete filesystem paths); this gateway owns only step (2). It is kept behind an interface so
 * `:cliApp` never constructs a platform [org.ooni.engine.SecureStorage] itself and tests can inject a
 * fake instead of touching a real keychain/secret store.
 *
 * Implementations MUST construct the platform secure storage lazily so a gateway that is created but
 * never used performs no native keychain/secret access.
 */
interface CliResetGateway {
    /**
     * Clears the scoped OONI secure storage (credentials keyed to the OONI app namespace).
     *
     * Throws if clearing fails so the CLI can abort filesystem deletion and exit nonzero — a failed
     * clear must never be followed by deleting the on-disk data.
     */
    suspend fun clearSecureStorage()

    fun close()
}

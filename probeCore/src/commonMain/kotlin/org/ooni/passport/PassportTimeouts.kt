package org.ooni.passport

/**
 * Timeouts (in seconds) for Passport HTTP calls, chosen per caller rather than globally.
 *
 * The split matters on a live-but-broken network (working transport, black-holed DNS): a
 * connection that never completes holds the call for the full timeout, so opportunistic work the
 * user didn't ask for must give up much sooner than work they're waiting on.
 */
object PassportTimeouts {
    /** Credentials, manifest, check-in and measurement submission - correctness-critical. */
    const val DEFAULT_SECONDS: Float = 30f

    /** Background prefetches: articles, descriptor updates, GeoIP lookups, proxy health. */
    const val PREFETCH_SECONDS: Float = 10f
}

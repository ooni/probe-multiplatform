package org.ooni.probe.shared.monitoring

/**
 * Builds the platform's real [InstrumentationDelegate] (Sentry-backed on full/xperimental builds,
 * no-op on fdroid). Installed into probeCore's [Instrumentation] at startup by `Dependencies`.
 */
expect fun createInstrumentationDelegate(): InstrumentationDelegate

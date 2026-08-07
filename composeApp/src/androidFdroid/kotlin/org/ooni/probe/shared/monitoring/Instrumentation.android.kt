package org.ooni.probe.shared.monitoring

actual fun createInstrumentationDelegate(): InstrumentationDelegate = NoOpInstrumentationDelegate

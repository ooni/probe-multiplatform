package org.ooni.probe.domain.auth

import org.ooni.passport.PassportBridge

fun bearerAuthHeader(sessionToken: String) = listOf(PassportBridge.KeyValue("Authorization", "Bearer $sessionToken"))

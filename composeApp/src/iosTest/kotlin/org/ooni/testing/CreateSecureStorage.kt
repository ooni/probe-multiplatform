package org.ooni.testing

import org.ooni.engine.IosSecureStorage
import org.ooni.engine.SecureStorage
import org.ooni.probe.config.OrganizationConfig

internal actual fun createTestSecureStorage(): SecureStorage = IosSecureStorage("${OrganizationConfig.appId}.testing")

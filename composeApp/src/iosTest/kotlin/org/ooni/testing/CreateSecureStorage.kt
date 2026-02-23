package org.ooni.testing

import org.ooni.engine.IosSecureStorage
import org.ooni.engine.SecureStorage

internal actual fun createTestSecureStorage(): SecureStorage = IosSecureStorage()

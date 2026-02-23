package org.ooni.testing

import org.ooni.engine.DesktopSecureStorage
import org.ooni.engine.SecureStorage

internal actual fun createTestSecureStorage(): SecureStorage = DesktopSecureStorage()

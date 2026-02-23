package org.ooni.testing

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.ooni.engine.AndroidSecureStorage
import org.ooni.engine.SecureStorage

internal actual fun createTestSecureStorage(): SecureStorage {
    val app = ApplicationProvider.getApplicationContext<Application>()
    return AndroidSecureStorage(app)
}

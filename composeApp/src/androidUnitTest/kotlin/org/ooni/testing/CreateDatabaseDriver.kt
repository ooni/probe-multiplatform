package org.ooni.testing

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.ooni.probe.Database

internal actual fun createTestDatabaseDriver(): SqlDriver {
    val app = ApplicationProvider.getApplicationContext<Application>()
    // With name = null it creates an in-memory database
    return AndroidSqliteDriver(Database.Schema, app, null)
}

package org.ooni.testing

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import org.ooni.probe.Database

internal actual fun createTestDatabaseDriver(): SqlDriver = inMemoryDriver(Database.Schema)

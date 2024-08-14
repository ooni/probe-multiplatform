package org.ooni.testing

import app.cash.sqldelight.db.SqlDriver

internal expect fun createTestDatabaseDriver(): SqlDriver

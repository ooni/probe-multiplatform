package org.ooni.probe.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import okio.Path
import okio.Path.Companion.toPath
import org.ooni.probe.Database

private const val DATABASE_FILE_NAME = "probe.db"

fun buildDatabaseDriver(folder: String): SqlDriver {
    val databasePath = folder.toPath().resolve(DATABASE_FILE_NAME)
    val driver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")
    val dbVersion = driver.getDatabaseVersion()
    val schemaVersion = Database.Schema.version

    if (dbVersion == null) {
        Logger.w("Database: deleting invalid database and re-creating.")
        driver.createDatabaseFromScratch(databasePath)
    } else if (dbVersion == 0L) {
        Logger.i("Database: creating database")
        driver.createDatabaseFromScratch(databasePath)
    } else if (schemaVersion > dbVersion) {
        Logger.i("Database: migrating from $dbVersion to $schemaVersion")
        try {
            Database.Schema.migrate(driver, dbVersion, schemaVersion)
            driver.setDatabaseVersion(schemaVersion)
        } catch (e: Exception) {
            Logger.w("Database: deleting invalid database and re-creating.", e)
            driver.createDatabaseFromScratch(databasePath)
        }
    } else {
        Logger.i("Database: up-to-date, no migration required")
    }

    return driver
}

private fun SqlDriver.createDatabaseFromScratch(databasePath: Path) {
    databasePath.toFile().delete()
    Database.Schema.create(this)
    setDatabaseVersion(Database.Schema.version)
}

private fun SqlDriver.getDatabaseVersion(): Long? =
    try {
        executeQuery(
            identifier = null,
            sql = "PRAGMA user_version;",
            mapper = { sqlCursor: SqlCursor -> QueryResult.Value(sqlCursor.getLong(0)) },
            parameters = 0,
            binders = null,
        ).value
    } catch (e: Exception) {
        Logger.w("Database: Could not get version", e)
        null
    }

private fun SqlDriver.setDatabaseVersion(version: Long) {
    try {
        execute(
            identifier = null,
            sql = "PRAGMA user_version = $version;",
            parameters = 0,
            binders = null,
        )
    } catch (e: Exception) {
        Logger.w("Database: Could set version to $version", e)
    }
}

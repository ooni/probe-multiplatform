package org.ooni.probe.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.ooni.probe.Database
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildDatabaseDriverTest {
    private lateinit var tempDir: java.nio.file.Path
    private lateinit var driver: SqlDriver

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("buildDriverTest")
        driver = buildDatabaseDriver(tempDir.toString())
    }

    @AfterTest
    fun tearDown() {
        driver.close()
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun walModeIsEnabled() {
        val journalMode = driver
            .executeQuery(
                null,
                "PRAGMA journal_mode;",
                { cursor -> QueryResult.Value(cursor.getString(0)) },
                0,
                null,
            ).value
        assertEquals("wal", journalMode)
    }

    @Test
    fun busyTimeoutIsSet() {
        val timeout = driver
            .executeQuery(
                null,
                "PRAGMA busy_timeout;",
                { cursor -> QueryResult.Value(cursor.getLong(0)) },
                0,
                null,
            ).value
        assertEquals(5000L, timeout)
    }

    @Test
    fun databaseSchemaIsCreated() {
        val version = driver
            .executeQuery(
                null,
                "PRAGMA user_version;",
                { cursor -> QueryResult.Value(cursor.getLong(0)) },
                0,
                null,
            ).value
        assertEquals(Database.Schema.version, version)
    }

    @Test
    fun concurrentReadsAndWritesDontThrowSqliteBusy() =
        runTest {
            val database = Database(driver)
            val writeJobs = (1..10).map { i ->
                launch(Dispatchers.IO) {
                    database.resultQueries.insertOrReplace(
                        id = null,
                        descriptor_name = "test_$i",
                        start_time = System.currentTimeMillis(),
                        is_viewed = 0L,
                        is_done = 1L,
                        data_usage_up = 0L,
                        data_usage_down = 0L,
                        failure_msg = null,
                        task_origin = "test",
                        network_id = null,
                        descriptor_runId = null,
                        descriptor_revision = null,
                        run_id = null,
                    )
                }
            }
            val readJobs = (1..10).map {
                launch(Dispatchers.IO) {
                    database.measurementQueries.selectAll().executeAsList()
                }
            }
            (writeJobs + readJobs).joinAll()
        }
}

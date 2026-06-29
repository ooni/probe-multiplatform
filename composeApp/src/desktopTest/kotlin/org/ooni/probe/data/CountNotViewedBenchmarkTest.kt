package org.ooni.probe.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.ooni.probe.Database
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CountNotViewedBenchmarkTest {
    private val report = StringBuilder()

    private fun line(text: String = "") {
        println(text)
        report.appendLine(text)
    }

    private val oldQuery = """
        SELECT COUNT(DISTINCT Result.id)
        FROM Result
        LEFT JOIN Measurement ON Measurement.result_id = Result.id
        WHERE Result.is_done = 1
          AND Result.is_viewed = 0
          AND Measurement.id IS NOT NULL AND Measurement.is_done = 1
    """.trimIndent()

    private val newQuery = """
        SELECT COUNT(*)
        FROM Result
        WHERE Result.is_done = 1
          AND Result.is_viewed = 0
          AND EXISTS (
              SELECT 1 FROM Measurement
              WHERE Measurement.result_id = Result.id
                AND Measurement.is_done = 1
          )
    """.trimIndent()

    @Test
    fun countAllNotViewedBenchmark() {
        val dbFile = Files.createTempFile("count_bench", ".db").toFile()
        dbFile.delete()
        val url = "jdbc:sqlite:${dbFile.absolutePath}"

        // Create the exact production schema (incl. all indexes) via SQLDelight, then drive raw JDBC.
        val driver = JdbcSqliteDriver(url)
        Database.Schema.create(driver)
        driver.close()

        try {
            DriverManager.getConnection(url).use { conn ->
                seedData(conn)
                runBenchmark(conn)
            }
        } finally {
            dbFile.delete()
        }
    }

    private fun seedData(conn: Connection) {
        conn.autoCommit = false
        val rnd = Random(1234)
        val nResults = 5_000

        val rps = conn.prepareStatement(
            "INSERT INTO Result(id, is_done, is_viewed, start_time) VALUES (?,?,?,?)",
        )
        val mps = conn.prepareStatement(
            "INSERT INTO Measurement(id, result_id, is_done, start_time) VALUES (?,?,?,?)",
        )

        var measId = 1L
        var unviewedDone = 0
        var totalMeas = 0
        for (rid in 1..nResults) {
            val isDone = if (rnd.nextDouble() < 0.8) 1 else 0
            val isViewed = if (rnd.nextDouble() < 0.7) 1 else 0
            if (isDone == 1 && isViewed == 0) unviewedDone++
            rps.setLong(1, rid.toLong())
            rps.setLong(2, isDone.toLong())
            rps.setLong(3, isViewed.toLong())
            rps.setLong(4, rid.toLong())
            rps.addBatch()

            // Heavy tail: 10% of results have many measurements (worst case for the JOIN).
            val count = if (rnd.nextDouble() < 0.1) 50 + rnd.nextInt(200) else 1 + rnd.nextInt(8)
            for (m in 0 until count) {
                val mDone = if (rnd.nextDouble() < 0.7) 1 else 0
                mps.setLong(1, measId++)
                mps.setLong(2, rid.toLong())
                mps.setLong(3, mDone.toLong())
                mps.setLong(4, rid.toLong())
                mps.addBatch()
                totalMeas++
            }
            if (rid % 1000 == 0) {
                rps.executeBatch()
                mps.executeBatch()
            }
        }
        rps.executeBatch()
        mps.executeBatch()
        conn.commit()
        rps.close()
        mps.close()

        line("countAllNotViewed benchmark — dataset: $nResults results, $totalMeas measurements, $unviewedDone unviewed+done")
        line()
    }

    private fun runBenchmark(conn: Connection) {
        val st = conn.createStatement()

        // Correctness: the rewritten query must return the same count as the original.
        setIndexes(st, result = "none", meas = "single")
        val oldCount = scalar(st, oldQuery)
        val newCount = scalar(st, newQuery)
        assertEquals(oldCount, newCount, "EXISTS rewrite must match the original LEFT JOIN query")
        line("correctness: oldCount=$oldCount newCount=$newCount (match)")
        line()

        // Regression guard: the shipped config must use both covering indexes.
        val shippedPlan = explain(st, newQuery, result = "composite", meas = "composite")
        line("shipped query plan (Result composite + Measurement composite):")
        shippedPlan.forEach { line("    $it") }
        line()
        val plan = shippedPlan.joinToString("\n")
        assertTrue(
            plan.contains("idx_result_is_done_is_viewed"),
            "Count query should use the Result(is_done, is_viewed) index",
        )
        assertTrue(
            plan.contains("idx_measure_result_id_is_done"),
            "EXISTS probe should use the Measurement(result_id, is_done) index",
        )
        assertTrue(
            plan.contains("COVERING INDEX idx_measure_result_id_is_done"),
            "EXISTS probe should be answered by the covering index without table reads",
        )

        // Timings (informational only).
        line("timings (median / p90 over $ITERS iters after $WARMUP warmup):")
        line("%-40s | %11s | %11s".format("config", "median (ms)", "p90 (ms)"))
        timeRow(st, "OLD  LEFT JOIN + COUNT(DISTINCT)", oldQuery, "none", "single")
        timeRow(st, "NEW  EXISTS (no new indexes)", newQuery, "none", "single")
        timeRow(st, "NEW  + Result(is_done,is_viewed)", newQuery, "composite", "single")
        timeRow(st, "NEW  + Measurement(result_id,is_done)", newQuery, "none", "composite")
        timeRow(st, "NEW  + both (shipped)", newQuery, "composite", "composite")
        st.close()
    }

    /**
     * Sets the index configuration for [Result] and [Measurement] so we can compare the
     * pre-PR layout ("single" = Measurement(result_id) only, no Result is_done/is_viewed index)
     * against the shipped layout.
     */
    private fun setIndexes(
        st: Statement,
        result: String,
        meas: String,
    ) {
        for (name in listOf(
            "idx_result_is_done_is_viewed",
            "idx_measure_result_id",
            "idx_measure_result_id_is_done",
        )) {
            st.execute("DROP INDEX IF EXISTS $name")
        }
        when (result) {
            "composite" -> st.execute("CREATE INDEX idx_result_is_done_is_viewed ON Result(is_done, is_viewed)")
        }
        when (meas) {
            "single" -> st.execute("CREATE INDEX idx_measure_result_id ON Measurement(result_id)")
            "composite" -> st.execute("CREATE INDEX idx_measure_result_id_is_done ON Measurement(result_id, is_done)")
        }
    }

    private fun scalar(
        st: Statement,
        sql: String,
    ): Long {
        st.executeQuery(sql).use { rs ->
            rs.next()
            return rs.getLong(1)
        }
    }

    private fun explain(
        st: Statement,
        sql: String,
        result: String,
        meas: String,
    ): List<String> {
        setIndexes(st, result, meas)
        val plan = mutableListOf<String>()
        st.executeQuery("EXPLAIN QUERY PLAN $sql").use { rs ->
            while (rs.next()) plan.add(rs.getString("detail"))
        }
        return plan
    }

    private fun timeRow(
        st: Statement,
        label: String,
        sql: String,
        result: String,
        meas: String,
    ) {
        setIndexes(st, result, meas)
        repeat(WARMUP) { scalar(st, sql) }
        val times = DoubleArray(ITERS)
        for (i in 0 until ITERS) {
            val t0 = System.nanoTime()
            scalar(st, sql)
            times[i] = (System.nanoTime() - t0) / 1_000_000.0
        }
        times.sort()
        line("%-40s | %11.3f | %11.3f".format(label, times[ITERS / 2], times[(ITERS * 0.9).toInt()]))
    }

    private companion object {
        const val WARMUP = 20
        const val ITERS = 80
    }
}

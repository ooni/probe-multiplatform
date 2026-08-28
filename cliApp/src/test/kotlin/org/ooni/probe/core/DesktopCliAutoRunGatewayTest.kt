package org.ooni.probe.core

import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopCliAutoRunGatewayTest {
    @Test
    fun freshStateReportsDisabledAutorunAndUnsatisfiedConstraintsWithSpecSummary() =
        runTest {
            // Fresh temp home: no onboarding, no autorun preferences, empty database. This is the
            // real gateway (no engine/native library) reading real bundled descriptors + real
            // (default) preferences + an empty results database.
            val dataDir = Files.createTempDirectory("cli-autorun-test")
            val prefsFile = dataDir.resolve("probe.preferences.json")
            val gateway = buildDesktopCliAutoRunGateway(
                CliStorageConfig(databaseDir = dataDir.toString(), preferencesFile = prefsFile.toString()),
            )
            try {
                val status = gateway.status()

                // Autorun is disabled by default (onboarding + upload + automated-testing all unset),
                // so the constraint check short-circuits to false through the canonical use case.
                assertFalse(status.enabled)
                assertFalse(status.wifiOnly)
                assertFalse(status.onlyWhileCharging)
                assertFalse(status.constraintsSatisfied)

                // The spec summary comes from GetAutoRunSpecification over the bundled OONI descriptors
                // with default (background-run-enabled) net tests, so it is non-empty and consistent.
                assertTrue(status.descriptorCount >= 1, "expected at least one autorun descriptor group")
                assertTrue(status.testCount >= status.descriptorCount, "test count should cover each group's net tests")
            } finally {
                gateway.close()
            }
        }

    @Test
    fun statusIsRepeatableAcrossCalls() =
        runTest {
            val dataDir = Files.createTempDirectory("cli-autorun-repeat")
            val prefsFile = dataDir.resolve("probe.preferences.json")
            val gateway = buildDesktopCliAutoRunGateway(
                CliStorageConfig(databaseDir = dataDir.toString(), preferencesFile = prefsFile.toString()),
            )
            try {
                assertEquals(gateway.status(), gateway.status())
            } finally {
                gateway.close()
            }
        }
}

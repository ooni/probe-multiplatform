package org.ooni.probe.cli

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CliRuntimeFailureTest {
    @Test
    fun conflictingConfigValuesFailBeforeGatewayConstruction() {
        withCli { cli, created, root ->
            val result = cli.run(
                args = arrayOf("--config", root.resolve("one.json").toString(), "info", "--config", root.resolve("two.json").toString()),
                stdout = {},
                stderr = {},
            )

            assertEquals(2, result)
            assertEquals(0, created())
        }
    }

    @Test
    fun invalidProxyFailsBeforeGatewayConstruction() {
        withCli { cli, created, _ ->
            val errors = mutableListOf<String>()
            val result = cli.run(arrayOf("info", "--proxy", "not-a-url"), {}, errors::add)

            assertEquals(2, result)
            assertTrue(errors.first().contains("Invalid proxy URL"))
            assertEquals(0, created())
        }
    }

    @Test
    fun invalidLogHandlerFailsBeforeGatewayConstruction() {
        withCli { cli, created, _ ->
            val result = cli.run(arrayOf("info", "--log-handler", "invalid"), {}, {})

            assertEquals(2, result)
            assertEquals(0, created())
        }
    }

    @Test
    fun batchWithExplicitLogHandlerFailsBeforeGatewayConstruction() {
        withCli { cli, created, _ ->
            listOf("cli", "batch", "syslog").forEach { handler ->
                val errors = mutableListOf<String>()
                val result = cli.run(arrayOf("info", "--batch", "--log-handler", handler), {}, errors::add)

                assertEquals(2, result)
                assertTrue(errors.first().contains("--batch cannot be combined"))
            }
            assertEquals(0, created())
        }
    }

    @Test
    fun configDirectoryFailsBeforeGatewayConstruction() {
        withCli { cli, created, root ->
            val directory = root.resolve("config-directory")
            Files.createDirectories(directory)
            val result = cli.run(arrayOf("--config", directory.toString(), "info"), {}, {})

            assertEquals(2, result)
            assertEquals(0, created())
        }
    }

    private fun withCli(block: (OoniprobeCli, () -> Int, Path) -> Unit) {
        val root = Files.createTempDirectory("cli-runtime-failure")
        try {
            var created = 0
            val runtime = CliRuntime.fromEnvironment(
                environment = mapOf("OONI_HOME" to root.resolve("home").toString()),
                userHome = root.resolve("user-home"),
                tempDir = root.resolve("temp"),
            )
            val cli = OoniprobeCli(
                runtime = runtime,
                coreGatewayFactory = CliCoreGatewayFactory {
                    created += 1
                    object : CliCoreGateway {}
                },
            )
            block(cli, { created }, root)
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}

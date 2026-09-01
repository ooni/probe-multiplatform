package org.ooni.probe.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import java.nio.file.Path
import org.ooni.probe.core.CliCoreGatewayDependencies
import org.ooni.probe.core.DescriptorAssetSet

class OoniprobeCliTest {
    private val runtime = CliRuntime(
        paths = CliPathLayout.create(
            ooniHome = Path.of("/tmp/ooni-home"),
            tempDir = Path.of("/tmp"),
        ),
    )

    @Test
    fun version() {
        val result = runCommand("version")

        assertEquals(0, result.code)
        assertEquals(listOf("OONI Probe ${CliBuildConfig.VERSION_NAME} (${CliBuildConfig.VERSION_CODE})"), result.stdout)
        assertEquals(emptyList(), result.stderr)
    }

    @Test
    fun info() {
        val result = runCommand("info")

        assertEquals(0, result.code)
        assertEquals(listOf("OONI home: /tmp/ooni-home", "Temp dir: /tmp"), result.stdout)
        assertEquals(emptyList(), result.stderr)
    }

    @Test
    fun unknownCommand() {
        val result = runCommand("bogus")

        assertEquals(2, result.code)
        assertEquals(emptyList(), result.stdout)
        assertTrue(result.stderr.first().contains("Unknown command: bogus"))
    }

    @Test
    fun helpCommand() {
        assertHelpResult(runCommand("help"))
    }

    @Test
    fun noArgumentsPrintHelp() {
        assertHelpResult(runCommand())
    }

    @Test
    fun longHelpOption() {
        assertHelpResult(runCommand("--help"))
    }

    @Test
    fun shortHelpOption() {
        assertHelpResult(runCommand("-h"))
    }

    @Test
    fun longVersionOption() {
        assertVersionResult(runCommand("--version"))
    }

    @Test
    fun shortVersionOption() {
        assertVersionResult(runCommand("-V"))
    }

    @Test
    fun extraArgumentsReturnUsageError() {
        val result = runCommand("version", "extra")

        assertEquals(2, result.code)
        assertEquals(emptyList(), result.stdout)
        assertTrue(result.stderr.isNotEmpty())
    }

    @Test
    fun coreGatewayFactoryIsInjectable() {
        val fakeGateway = object : CliCoreGateway {}
        val cli = OoniprobeCli(
            runtime = runtime,
            coreGatewayFactory = CliCoreGatewayFactory { fakeGateway },
        )

        assertSame(fakeGateway, cli.createCoreGateway())
    }

    @Test
    fun productionGatewayComesFromProbeCoreAndDefaultsToOoniDescriptors() {
        val gateway = OoniprobeCli(runtime).createCoreGateway()

        assertTrue(gateway is CliCoreGatewayDependencies)
        assertEquals(DescriptorAssetSet.Ooni, gateway.defaultDescriptorAssetSet)
        assertTrue(gateway.javaClass.name.startsWith("org.ooni.probe.core."))
    }

    @Test
    fun rootOptionsCanAppearBeforeOrAfterInfo() {
        val configFile = "/tmp/external/config.json"
        val before = runCommand("--config", configFile, "info", "--json")
        val after = runCommand("info", "--json", "--config", configFile)

        assertEquals(0, before.code)
        assertEquals(before, after)
        assertTrue(before.stdout.single().contains("\"config_file\":\"$configFile\""))
    }

    @Test
    fun runtimeOptionsPopulateInfoJson() {
        val result = runCommand(
            "info",
            "--json",
            "--config", "/tmp/config.json",
            "--software-name", "custom-probe",
            "--software-version", "1.2.3",
            "--proxy", "http://127.0.0.1:8080",
            "--batch",
            "--verbose",
        )

        assertEquals(0, result.code)
        assertTrue(result.stdout.single().contains("\"software_name\":\"custom-probe\""))
        assertTrue(result.stdout.single().contains("\"software_version\":\"1.2.3\""))
        assertTrue(result.stdout.single().contains("\"proxy\":\"http://127.0.0.1:8080\""))
        assertTrue(result.stdout.single().contains("\"batch\":true"))
        assertTrue(result.stdout.single().contains("\"verbose\":true"))
        assertTrue(result.stdout.single().contains("\"log_handler\":\"batch\""))
    }

    @Test
    fun infoJsonEscapesQuotedAndControlRuntimeStrings() {
        val softwareName = "bad\"name\\folder\nnext"
        val softwareVersion = "v\"1\\build\rnext"
        val configFile = "/tmp/config\"name.json"
        val result = runCommand(
            "info",
            "--json",
            "--config", configFile,
            "--software-name", softwareName,
            "--software-version", softwareVersion,
        )

        assertEquals(0, result.code)
        assertEquals(
            """{"ooni_home":"/tmp/ooni-home","config_file":"/tmp/config\"name.json","temp_dir":"/tmp","software_name":"bad\"name\\folder\nnext","software_version":"v\"1\\build\rnext","proxy":null,"batch":false,"verbose":false,"log_handler":"cli"}""",
            result.stdout.single(),
        )
    }

    private fun assertHelpResult(result: CommandResult) {
        assertEquals(0, result.code)
        assertEquals(
            listOf(
                """
                Usage: ooniprobe <command>

                Commands:
                  version   Print CLI version
                  info      Print local runtime paths
                  geoip     Print the probe's current network location
                  list      List results, or measurements for a result
                  show      Print a stored measurement as JSON
                  rm        Delete results
                  reset     Delete all local OONI Probe data
                  onboard   Accept the informed-consent onboarding
                  run       Run measurement groups
                  upload    Upload measurements not yet submitted
                  autorun   Inspect autorun status and logs
                  internal  Hidden diagnostics
                  help      Print this help
                """.trimIndent(),
            ),
            result.stdout,
        )
        assertEquals(emptyList(), result.stderr)
    }

    private fun assertVersionResult(result: CommandResult) {
        assertEquals(0, result.code)
        assertEquals(listOf("OONI Probe ${CliBuildConfig.VERSION_NAME} (${CliBuildConfig.VERSION_CODE})"), result.stdout)
        assertEquals(emptyList(), result.stderr)
    }

    private fun runCommand(vararg args: String): CommandResult {
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()
        val code = OoniprobeCli(runtime).run(
            args = args.toList().toTypedArray(),
            stdout = stdout::add,
            stderr = stderr::add,
        )

        return CommandResult(code, stdout, stderr)
    }

    private data class CommandResult(
        val code: Int,
        val stdout: List<String>,
        val stderr: List<String>,
    )
}

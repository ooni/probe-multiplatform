package org.ooni.probe.background

import co.touchlab.kermit.Logger
import org.ooni.probe.platform
import org.ooni.probe.shared.DesktopOS

fun registerWindowsUrlScheme() {
    if (platform.os != DesktopOS.Windows) return

    val exePath = ProcessHandle
        .current()
        .info()
        .command()
        .orElse("unknown")

    val keyPath = """HKCU\Software\Classes\ooni"""
    val checkCommand = arrayOf("reg", "query", """"$keyPath\shell\open\command"""", "/ve")

    Logger.d("Checking if OONI URL scheme is already registered...")
    val result = runCommand(checkCommand)
    if (result.success) {
        val expectedCommand = """"$exePath" "%1""""
        if (result.output.contains(expectedCommand)) {
            Logger.d("OONI URL scheme is already registered and points to the correct executable: $exePath")
            return // Scheme is correctly registered
        } else {
            Logger.d(
                "OONI URL scheme is registered but points to a different command. Expected: '$expectedCommand'. Will update.",
            )
        }
    }

    val commands = listOf(
        arrayOf("reg", "add", """"$keyPath"""", "/ve", "/d", "\"OONI Run\"", "/f"),
        arrayOf("reg", "add", """"$keyPath"""", "/v", "\"URL Protocol\"", "/f"),
        arrayOf("reg", "add", """"$keyPath\shell"""", "/f"),
        arrayOf("reg", "add", """"$keyPath\shell\open"""", "/f"),
        arrayOf("reg", "add", """"$keyPath\shell\open\command"""", "/ve", "/d", """"\"$exePath\" \"%1\""""", "/f"),
    )

    Logger.d("Registering OONI URL scheme...")
    for (cmd in commands) {
        runCommand(cmd)
    }
}

data class CommandResult(
    val success: Boolean,
    val output: String,
)

fun runCommand(command: Array<String>): CommandResult =
    try {
        val process = Runtime.getRuntime().exec(command)
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        if (exitCode == 0) {
            Logger.d("Command succeeded: ${command.joinToString(" ")}")
            CommandResult(true, output)
        } else {
            Logger.d("Command failed: ${command.joinToString(" ")}. Error: $errorOutput")
            CommandResult(false, errorOutput)
        }
    } catch (e: Exception) {
        Logger.e("Failed to execute command: ${command.joinToString(" ")}", e)
        CommandResult(false, e.message ?: "Unknown error")
    }

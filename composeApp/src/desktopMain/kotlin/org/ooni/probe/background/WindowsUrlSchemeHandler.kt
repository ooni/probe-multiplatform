package org.ooni.probe.background

import co.touchlab.kermit.Logger
import org.ooni.probe.platform
import org.ooni.probe.shared.DesktopOS
import java.io.IOException

fun registerWindowsUrlScheme() {
    if (platform.os != DesktopOS.Windows) return

    val exePath = ProcessHandle
        .current()
        .info()
        .command()
        .orElse("unknown")

    val keyPath = """HKCU\Software\Classes\ooni"""
    val checkCommand = """reg query "$keyPath" /ve"""

    try {
        // Check if the key already exists to avoid unnecessary writes
        val checkProcess = Runtime.getRuntime().exec(checkCommand)
        if (checkProcess.waitFor() == 0) {
            Logger.d("OONI URL scheme already registered.")
            return
        }
    } catch (e: IOException) {
        Logger.e("Failed to check registry key", e)
        // Proceed to attempt registration anyway
    }

    val commands = listOf(
        """reg add "$keyPath" /ve /d "OONI Run" /f""",
        """reg add "$keyPath" /v "URL Protocol" /f""",
        """reg add "$keyPath\shell" /f""",
        """reg add "$keyPath\shell\open" /f""",
        """reg add "$keyPath\shell\open\command" /ve /d "\"$exePath\" \"%1\"" /f""",
    )

    Logger.d("Registering OONI URL scheme...")
    for (cmd in commands) {
        try {
            val process = Runtime.getRuntime().exec(cmd)
            process.waitFor()
            if (process.exitValue() != 0) {
                Logger.d("Command failed: $cmd")
                process.errorStream
                    .bufferedReader()
                    .use { it.lines().forEach { line -> Logger.d(line) } }
            } else {
                Logger.d("Command succeeded: $cmd")
            }
            Logger.d("Executed command: $cmd")
        } catch (e: IOException) {
            Logger.e("Failed to execute command: $cmd", e)
        }
    }
}

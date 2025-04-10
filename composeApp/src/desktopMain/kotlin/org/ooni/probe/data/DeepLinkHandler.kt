package org.ooni.probe.data

import co.touchlab.kermit.Logger
import org.ooni.probe.APP_ID
import tk.pratanumandal.unique4j.Unique4j
import tk.pratanumandal.unique4j.exception.Unique4jException
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Date


class DeepLinkHandler {

    private var unique: Unique4j? = null
    private val messageListeners = mutableListOf<(String?) -> Unit>()

    fun addMessageListener(listener: (String?) -> Unit) {
        messageListeners.add(listener)
    }

    fun parseDeepLink(deepLink: String): Map<String, String> {
        val params = mutableMapOf<String, String>()

        if (deepLink.startsWith("ooni://")) {
            val parts = deepLink.removePrefix("ooni://").split("/")
            if (parts.size >= 2 && parts[0] == "runv2") {
                params["command"] = "runv2"
                params["testId"] = parts[1]
            }
        }

        return params
    }

    fun initialize(args: Array<String>) {
        try {
            unique = object : Unique4j(APP_ID) {
                override fun receiveMessage(message: String) {
                    Logger.d("Received message: $message")

                    // Parse deep link if it starts with ooni://
                    if (message.startsWith("ooni://")) {
                        val params = parseDeepLink(message)
                        handleDeepLinkCommand(message, params)
                    }
                }

                override fun sendMessage(): String {
                    // Check for deep links in arguments
                    val deepLink = findDeepLinkInArgs(args)
                    return deepLink ?: "Application launched without deep link at ${Date()}"
                }

                override fun handleException(exception: Exception) {
                    Logger.e(exception) {"Exception occurred"}
                }

                override fun beforeExit() {
                    Logger.d("Exiting subsequent instance.")
                }
            }

            // Try to acquire lock
            val lockFlag = unique!!.acquireLock()
            if (lockFlag) {
                Logger.d("Application instance started successfully")

                // If this is the first instance and has a deep link, handle it directly
                val deepLink = findDeepLinkInArgs(args)
                if (deepLink != null) {
                    val params = parseDeepLink(deepLink)
                    // messageListeners.forEach { it("Initial deep link: $deepLink, params: $params") }
                    handleDeepLinkCommand(deepLink, params)
                }

                // TODO: Implement with conveyor
                // Register as a protocol handler if not already registered
                registerProtocolHandler()
            } else {
                Logger.d("Could not acquire lock - this should not happen")
            }

            // Register shutdown hook to free the lock when application exits
            Runtime.getRuntime().addShutdownHook(Thread {
                unique?.freeLock()
            })

        } catch (e: Unique4jException) {
            Logger.e(e) { "Failed to initialize Unique4j" }
        }
    }

    private fun findDeepLinkInArgs(args: Array<String>): String? {
        // Windows may process URLs differently, so handle various formats
        for (arg in args) {
            // Standard format
            if (arg.startsWith("ooni://")) {
                return arg
            }

            // Windows can sometimes pass URLs with the protocol separated
            if (arg.equals("ooni:", ignoreCase = true) && args.size > 1) {
                return "ooni://${args[1]}"
            }

            // Windows can sometimes add extra slashes
            if (arg.startsWith("ooni:////") || arg.startsWith("ooni:/\\")) {
                return "ooni://" + arg.substring(arg.indexOf("ooni:") + 7)
            }
        }

        return null
    }

    private fun handleDeepLinkCommand(deepLink: String, params: Map<String, String>) {
        // Handle different commands based on the deep link
        when (params["command"]) {
            "runv2" -> {
                val testId = params["testId"]
                Logger.d("Running test with ID: $testId")
                // Here you would implement the actual test running logic
                messageListeners.forEach { it(testId) }
            }
        }
    }

    fun shutdown() {
        unique?.freeLock()
    }

    //TODO: Move this to conveyor

    // Register the application as a handler for ooni:// protocol
    private fun registerProtocolHandler() {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")

        if (isWindows) {
            registerWindowsProtocolHandler()
        } else {
            registerLinuxProtocolHandler()
        }
    }

    // Register protocol handler for Linux using XDG
    private fun registerLinuxProtocolHandler() {
        try {
            // Get the current executable path
            val executablePath = getExecutablePath()
            if (executablePath != null) {
                // Create desktop entry for protocol handler
                val desktopEntryPath = Paths.get(
                    System.getProperty("user.home"),
                    ".local",
                    "share",
                    "applications",
                    "ooni-handler.desktop"
                )

                val desktopEntry = """
                    [Desktop Entry]
                    Type=Application
                    Name=OONI Deep Link Handler
                    Exec=$executablePath %u
                    Terminal=false
                    MimeType=x-scheme-handler/ooni;
                    NoDisplay=true
                    Comment=Handler for OONI deep links
                """.trimIndent()

                // Create parent directories if they don't exist
                Files.createDirectories(desktopEntryPath.parent)

                // Write the desktop entry file
                Files.write(desktopEntryPath, desktopEntry.toByteArray())

                // Register the desktop entry as the handler for the ooni:// protocol
                try {
                    val processBuilder = ProcessBuilder(
                        "xdg-mime", "default", "ooni-handler.desktop", "x-scheme-handler/ooni"
                    )
                    val process = processBuilder.start()
                    process.waitFor()
                    Logger.d("Registered as handler for ooni:// protocol on Linux")
                } catch (e: Exception) {
                    Logger.d("Failed to register protocol handler on Linux: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to register protocol handler on Linux" }
        }
    }

    // Register protocol handler for Windows using Registry
    private fun registerWindowsProtocolHandler() {
        try {
            val executablePath = getExecutablePath()
            if (executablePath != null) {
                // Escape backslashes in the path for command prompt
                val escapedPath = executablePath.replace("\\", "\\\\")

                // Create the registry entry for the ooni:// protocol
                val command = """
                    reg add "HKCU\Software\Classes\ooni" /ve /d "URL:OONI Protocol" /f
                    reg add "HKCU\Software\Classes\ooni" /v "URL Protocol" /d "" /f
                    reg add "HKCU\Software\Classes\ooni\DefaultIcon" /ve /d "$escapedPath,1" /f
                    reg add "HKCU\Software\Classes\ooni\shell\open\command" /ve /d "\"$escapedPath\" \"%1\"" /f
                """.trimIndent()

                // Execute the registry command
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("cmd.exe", "/c", command))
                    val exitCode = process.waitFor()

                    if (exitCode == 0) {
                        Logger.d("Successfully registered ooni:// protocol handler on Windows")
                    } else {
                        Logger.d("Failed to register protocol handler on Windows, exit code: $exitCode")
                    }
                } catch (e: Exception) {
                    Logger.e(e) { "Failed to execute registry command" }
                }
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to register protocol handler on Windows" }
        }
    }

    // Get the path to the current executable based on the operating system
    private fun getExecutablePath(): String? {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")

        // Try to get the path from the conveyor system property if using Conveyor
        val conveyorPath = System.getProperty("app.home")
        if (conveyorPath != null) {
            val conveyorExecutable = if (isWindows) {
                File(conveyorPath, "bin/app.exe")
            } else {
                File(conveyorPath, "bin/app")
            }

            if (conveyorExecutable.exists()) {
                return conveyorExecutable.absolutePath
            }
        }

        if (isWindows) {
            // For Windows, try to use jpackage executable path
            val processPath = System.getProperty("jpackage.app-path")
            if (processPath != null) {
                return processPath
            }

            // Alternatively, use the Java executable with classpath
            val javaHome = System.getProperty("java.home")
            val javaBin = File(javaHome, "bin/java.exe").absolutePath
            val classpath = System.getProperty("java.class.path")
            return "\"$javaBin\" -cp \"$classpath\" MainKt"
        } else {
            // For Linux, try to get the symbolic link of the current process
            try {
                val currentPid = ProcessHandle.current().pid()
                val processPath = Files.readSymbolicLink(Paths.get("/proc/$currentPid/exe"))
                return processPath.toString()
            } catch (e: Exception) {
                Logger.e(e) { "Could not determine executable path" }
            }

            // If all else fails, just use java with the classpath
            return "java -cp ${System.getProperty("java.class.path")} MainKt"
        }
    }

}

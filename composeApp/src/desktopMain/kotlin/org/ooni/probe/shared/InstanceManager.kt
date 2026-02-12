package org.ooni.probe.shared

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.ooni.probe.APP_ID
import tk.pratanumandal.unique4j.Unique4j
import tk.pratanumandal.unique4j.exception.Unique4jException
import java.awt.Desktop
import java.net.URI
import java.util.Date

// Ensures only one app instance is running
// and relays the URLs (deep links) it receives from the system
class InstanceManager(
    private val platformInfo: PlatformInfo,
) {
    private var unique: Unique4j? = null
    private val urls = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val activations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun initialize(args: Array<String>) {
        // MacOS already only allows for one app instance
        if (isMac) {
            // setOpenURIHandler only supports Mac
            Desktop.getDesktop().setOpenURIHandler { event ->
                urls.tryEmit(event.uri.toString())
            }
            return
        }

        try {
            unique = object : Unique4j(APP_ID) {
                override fun receiveMessage(message: String) {
                    activations.tryEmit(Unit)
                    handleArgs(message.split(" ").toTypedArray())
                }

                override fun sendMessage(): String {
                    // Check for deep links in arguments
                    Logger.d("Application launched without deep link at ${Date()}")
                    return args.joinToString(" ")
                }

                override fun handleException(exception: Exception) {
                    Logger.e(exception) { "Exception occurred" }
                }

                override fun beforeExit() {
                    Logger.d("Exiting subsequent instance.")
                }
            }

            // Try to acquire lock
            val lockFlag = unique!!.acquireLock()
            if (lockFlag) {
                Logger.d("Application instance started successfully")
                handleArgs(args)
            } else {
                Logger.d("Could not acquire lock - this should not happen")
            }

            // Register shutdown hook to free the lock when application exits
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    unique?.freeLock()
                },
            )
        } catch (e: Unique4jException) {
            Logger.e("Failed to initialize Unique4j", e)
        }
    }

    fun shutdown() {
        unique?.freeLock()
    }

    fun observeUrls() = urls.asSharedFlow()

    fun observeActivation() = activations.asSharedFlow()

    private fun handleArgs(args: Array<String>) {
        findUrlInArgs(args)?.let {
            urls.tryEmit(it)
        }
    }

    private fun findUrlInArgs(args: Array<String>): String? {
        args.forEach { arg ->
            val sanitizedArg =
                // Windows can sometimes add extra slashes
                arg
                    .replace(":////", "://")
                    .replace(":/\\", "://")

            try {
                URI.create(sanitizedArg) // parse URL
                return sanitizedArg
            } catch (_: Exception) {
            }
        }

        // Windows can sometimes pass URLs with the protocol separated
        if (args.size >= 2 && args.first().endsWith(":")) {
            return args[0] + "//" + args[1]
        }

        return null
    }

    private val isMac get() = (platformInfo.platform as? Platform.Desktop)?.os == DesktopOS.Mac
}

package org.ooni.probe.data

import co.touchlab.kermit.Logger
import org.ooni.probe.APP_ID
import org.ooni.probe.data.models.DeepLink
import tk.pratanumandal.unique4j.Unique4j
import tk.pratanumandal.unique4j.exception.Unique4jException
import java.util.Date

class DeepLinkHandler {
    private var unique: Unique4j? = null
    private val messageListeners = mutableListOf<(DeepLink.AddDescriptor?) -> Unit>()

    companion object {
        private const val OONI_PROTOCOL = "ooni://"
    }

    fun addMessageListener(listener: (DeepLink.AddDescriptor?) -> Unit) {
        messageListeners.add(listener)
    }

    fun parseDeepLink(deepLink: String): DeepLink.AddDescriptor? {
        return deepLink.takeIf { it.startsWith(OONI_PROTOCOL) }
            ?.removePrefix(OONI_PROTOCOL)
            ?.split("/")
            ?.takeIf { it.size >= 2 && it[0] == "runv2" }
            ?.let { DeepLink.AddDescriptor(it[1]) }
    }

    fun initialize(args: Array<String>) {
        try {
            unique = object : Unique4j(APP_ID) {
                override fun receiveMessage(message: String) {
                    Logger.d("Received message: $message")

                    parseDeepLink(message)?.let { deepLink ->
                        messageListeners.forEach { listener ->
                            listener(deepLink)
                        }
                    }
                }

                override fun sendMessage(): String {
                    // Check for deep links in arguments
                    val deepLink = findDeepLinkInArgs(args)
                    return deepLink ?: "Application launched without deep link at ${Date()}"
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

                findDeepLinkInArgs(args)?.let { deepLink ->
                    parseDeepLink(deepLink)?.let { deepLink ->
                        messageListeners.forEach { listener ->
                            listener(deepLink)
                        }
                    }
                }
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
            Logger.e(e) { "Failed to initialize Unique4j" }
        }
    }

    private fun findDeepLinkInArgs(args: Array<String>): String? {
        // Windows may process URLs differently, so handle various formats
        for (arg in args) {
            // Standard format
            if (arg.startsWith(OONI_PROTOCOL)) {
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

    fun shutdown() {
        unique?.freeLock()
    }
}

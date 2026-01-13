package org.ooni.probe.shared

import co.touchlab.kermit.Logger
import org.ooni.probe.platform
import org.ooni.shared.loadNativeLibrary

/**
 * Utility for controlling macOS dock icon visibility using JNI.
 * Calls native macOS functions through NSApp.setActivationPolicy.
 */
object MacDockVisibility {
    private var isNativeLibraryLoaded = false

    init {
        if (isMacOS()) {
            loadNativeLibrary()
        }
    }

    /**
     * Shows the dock icon (makes app visible in dock)
     * Calls native: NSApp.setActivationPolicy(.regular)
     */
    fun showDockIcon() {
        if (!isMacOS()) {
            Logger.d("MacDockVisibility: Not on macOS, skipping dock icon show")
            return
        }

        if (!isNativeLibraryLoaded) {
            Logger.w("MacDockVisibility: Native library not loaded, cannot show dock icon")
            return
        }

        try {
            showInDockNative()
            Logger.d("MacDockVisibility: Successfully showed dock icon via JNI")
        } catch (e: Exception) {
            Logger.w("MacDockVisibility: Failed to show dock icon via JNI", e)
        }
    }

    /**
     * Hides the dock icon (makes app invisible in dock)
     * Calls native: NSApp.setActivationPolicy(.accessory)
     */
    fun hideDockIcon() {
        if (!isMacOS()) {
            Logger.d("MacDockVisibility: Not on macOS, skipping dock icon hide")
            return
        }

        if (!isNativeLibraryLoaded) {
            Logger.w("MacDockVisibility: Native library not loaded, cannot hide dock icon")
            return
        }

        try {
            removeFromDockNative()
            Logger.d("MacDockVisibility: Successfully hid dock icon via JNI")
        } catch (e: Exception) {
            Logger.w("MacDockVisibility: Failed to hide dock icon via JNI", e)
        }
    }

    /**
     * Sets the dock icon visibility based on window visibility
     */
    fun setDockIconVisible(visible: Boolean) {
        if (visible) {
            showDockIcon()
        } else {
            hideDockIcon()
        }
    }

    private fun isMacOS(): Boolean = platform.os == DesktopOS.Mac

    private fun loadNativeLibrary() {
        try {
            isNativeLibraryLoaded = loadNativeLibrary("macdockvisibility")
            if (isNativeLibraryLoaded) {
                Logger.d("MacDockVisibility: Successfully loaded native library")
            } else {
                Logger.w("MacDockVisibility: Failed to load native library")
            }
        } catch (e: Exception) {
            Logger.w("MacDockVisibility: Exception while loading native library", e)
            isNativeLibraryLoaded = false
        }
    }

    // JNI native method declarations
    @JvmStatic
    private external fun showInDockNative()

    @JvmStatic
    private external fun removeFromDockNative()
}

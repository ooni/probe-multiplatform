package org.ooni.probe.ui.shared

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

class ClipboardActions(
    private val snackbarHostState: SnackbarHostState?,
    private val clipboard: Clipboard,
) {
    suspend fun copyToClipboard(message: String) {
        clipboard.setClipEntry(message.toClipEntry())
        snackbarHostState?.showSnackbar("Copied to clipboard")
    }
}

val LocalClipboardActions = compositionLocalOf<ClipboardActions?> { null }

expect fun String.toClipEntry(): ClipEntry

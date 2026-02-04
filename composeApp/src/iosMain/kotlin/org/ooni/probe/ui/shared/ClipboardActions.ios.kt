package org.ooni.probe.ui.shared

import androidx.compose.ui.platform.ClipEntry

actual fun String.toClipEntry(): ClipEntry = ClipEntry.withPlainText(this)

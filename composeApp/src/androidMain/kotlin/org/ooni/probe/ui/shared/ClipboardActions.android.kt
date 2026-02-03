package org.ooni.probe.ui.shared

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry

actual fun String.toClipEntry(): ClipEntry = ClipData.newPlainText(null, this).toClipEntry()

package org.ooni.probe.data.models

import okio.Path

data class FileSharing(
    val title: String,
    val filePath: Path,
)

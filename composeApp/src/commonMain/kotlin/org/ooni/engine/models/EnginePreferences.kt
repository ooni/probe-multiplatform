package org.ooni.engine.models

import kotlin.time.Duration

data class EnginePreferences(
    val enabledWebCategories: List<String>,
    val taskLogLevel: TaskLogLevel,
    val uploadResults: Boolean,
    val proxy: String?,
    val maxRuntime: Duration?,
)

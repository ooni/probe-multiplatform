package org.ooni.probe.di

import kotlinx.serialization.json.Json
import org.ooni.engine.Engine
import org.ooni.engine.OonimkallBridge
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.ui.main.MainViewModel

class Dependencies(
    val platformInfo: PlatformInfo,
    private val oonimkallBridge: OonimkallBridge,
    private val baseFileDir: String,
) {
    private val json by lazy {
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }

    private val engine by lazy { Engine(oonimkallBridge, json, baseFileDir) }

    val mainViewModel by lazy { MainViewModel(engine) }
}

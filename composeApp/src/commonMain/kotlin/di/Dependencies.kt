package di

import platform.GoOONIProbeClientBridge
import ui.main.MainViewModel

class Dependencies(
    private val goOONIProbeClientBridge: GoOONIProbeClientBridge
) {

    val mainViewModel by lazy { MainViewModel(goOONIProbeClientBridge) }

}
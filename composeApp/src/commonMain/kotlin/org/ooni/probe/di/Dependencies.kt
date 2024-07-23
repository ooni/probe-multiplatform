package org.ooni.probe.di

import org.ooni.engine.OoniEngine
import org.ooni.probe.ui.main.MainViewModel

class Dependencies(
    private val engine: OoniEngine
) {

    val mainViewModel by lazy { MainViewModel(engine) }

}
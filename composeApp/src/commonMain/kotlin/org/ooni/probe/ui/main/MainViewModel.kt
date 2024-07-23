package org.ooni.probe.ui.main

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.ooni.engine.OoniEngine

class MainViewModel(
    private val engine: OoniEngine
) {
    private val _result = MutableStateFlow<String?>(null)
    val result = _result.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            _result.value = engine.newUUID4()
        }
    }
}

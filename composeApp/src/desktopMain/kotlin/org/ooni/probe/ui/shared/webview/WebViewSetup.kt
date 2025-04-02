package org.ooni.probe.ui.shared.webview

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import java.io.File
import dev.datlag.kcef.KCEF

class WebViewSetup(
    private val filesDir: String,
    private val cacheDir: String,
    private val backgroundContext: CoroutineContext = Dispatchers.IO
) {

    private val _state = MutableStateFlow<WebViewState>(WebViewState.NotInitialized)
    val state = _state.asStateFlow()

    suspend fun initialize() {
        withContext(backgroundContext) {
            val bundleLocation = File(filesDir)

            try {
                KCEF.init(builder = {
                    installDir(File(bundleLocation, "kcef-bundle"))

                    progress {
                        onDownloading {
                            _state.value = WebViewState.Downloading(it.coerceAtLeast(0f))
                        }
                        onInitialized {
                            _state.value = WebViewState.Initialized
                        }
                    }
                    settings {
                        cachePath = cacheDir
                    }
                }, onError = {
                    Logger.e("Error initializing WebView", it)
                    _state.value = WebViewState.Error
                }, onRestartRequired = {
                    _state.value = WebViewState.RestartRequired // Rare
                })
            } catch (e: Exception) {
                Logger.e("Error initializing WebView", e)
                _state.value = WebViewState.Error
            }
        }
    }
}

sealed interface WebViewState {
    data object NotInitialized : WebViewState
    data object Error : WebViewState
    data class Downloading(val progress: Float) : WebViewState
    data object RestartRequired : WebViewState
    data object Initialized : WebViewState
}

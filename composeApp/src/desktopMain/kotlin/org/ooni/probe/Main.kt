package org.ooni.probe

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource
import java.io.File

fun main() {
    application {
        Window(
            title = stringResource(Res.string.app_name),
            state = rememberWindowState(size = DpSize(560.dp, 1024.dp)),
            onCloseRequest = ::exitApplication,
        ) {
            App(
                dependencies = dependencies,
                deepLink = null,
                onDeeplinkHandled = {},
            )

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    Logger.i("init")
                    KCEF.init(builder = {
                        installDir(File("kcef-bundle"))

                        /*
                          Add this code when using JDK 17.
                          Builder().github {
                              release("jbr-release-17.0.10b1087.23")
                          }.buffer(download.bufferSize).build()
                         */
                        progress {
                            onDownloading {
                                Logger.i("Downloading $it")
                            }
                            onInitialized {
                                Logger.i("onInitialized")
                            }
                        }
                        settings {
                            cachePath = File("cache").absolutePath
                        }
                    }, onError = {
                        Logger.i("onError $it")
                        it?.printStackTrace()
                    })
                }
            }
        }
    }
}

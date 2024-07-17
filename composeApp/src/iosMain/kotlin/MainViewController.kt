import androidx.compose.ui.window.ComposeUIViewController
import di.Dependencies
import platform.IosOONIProbeClientBridge

fun MainViewController() = ComposeUIViewController { App(Dependencies(IosOONIProbeClientBridge())) }
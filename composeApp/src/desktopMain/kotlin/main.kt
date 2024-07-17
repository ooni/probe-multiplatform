import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import di.Dependencies
import platform.DesktopOONIProbeClientBridge

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "OONI Probe",
    ) {
        App(Dependencies(DesktopOONIProbeClientBridge()))
    }
}
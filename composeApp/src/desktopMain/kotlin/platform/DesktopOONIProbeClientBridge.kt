package platform

class DesktopOONIProbeClientBridge : GoOONIProbeClientBridge {
    override fun apiCall(funcName: String) = "Desktop Result"
    override fun apiCallWithArgs(funcName: String, args : String) = ""
}
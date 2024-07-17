package platform

class IosOONIProbeClientBridge : GoOONIProbeClientBridge {
    override fun apiCall(funcName: String) = "ios result"
    override fun apiCallWithArgs(funcName: String, args : String) = ""
}
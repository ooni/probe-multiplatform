package platform

expect class GoOONIProbeClientBridge {

    fun apiCall(funcName: String): String
    fun apiCallWithArgs(funcName: String, args : String): String

}
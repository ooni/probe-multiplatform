package org.ooni.probe

import android.content.Context
import io.github.aakira.napier.Napier

class GoOONIProbeClient(context: Context) {
    init {
        Napier.d("loading ooniprobe in appContext=$context")
        // System.loadLibrary() is the most basic shared library loading strategy.
        // I have seen code like the one in Wireguard make use of many more:
        // https://github.com/WireGuard/wireguard-android/blob/master/tunnel/src/main/java/com/wireguard/android/util/SharedLibraryLoader.java
        // TODO: do testing of this and unnderstand if these strategies are needed or if it only
        //  applies to older versions of android and/or loading happening inside of a VPNService
        System.loadLibrary("ooniprobe")
    }
    companion object {
        @JvmStatic private external fun apiCall(funcName: String): String
        @JvmStatic private external fun apiCallWithArgs(funcName: String, args : String): String
    }
    fun call(funcName: String) : String{
        return apiCall(funcName)
    }
    fun callWithArgs(funcName: String, args : String) : String{
        return apiCallWithArgs(funcName, args)
    }

}

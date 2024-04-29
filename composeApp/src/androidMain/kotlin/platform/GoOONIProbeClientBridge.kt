package platform

import android.content.Context
import io.github.aakira.napier.Napier
import org.ooni.probe.GoOONIProbeClient

actual class GoOONIProbeClientBridge(context: Context) {
    private val client = GoOONIProbeClient(context)
    init {
        Napier.d("running laoder")
    }

    actual fun apiCall(funcName: String): String {
        Napier.d("running API call ${funcName}")
        return client.call(funcName)
    }
    actual fun apiCallWithArgs(funcName: String, args : String): String {
        Napier.d("running API call with args ${funcName} ${args}")
        return client.callWithArgs(funcName, args)
    }
}
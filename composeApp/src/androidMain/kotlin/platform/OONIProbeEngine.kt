package platform

import android.content.Context
import io.github.aakira.napier.Napier
import jni.OONIProbeEngineJNI
import org.internetok.iokapp.loadSharedLibrary

actual class OONIProbeEngine(private val context: Context) {
    actual fun demoCheck() : String {
        Napier.i("loading probeengine library")
        System.loadLibrary("probeengine")
        return OONIProbeEngineJNI.demoCheck()
    }

}
package jni

import java.nio.ByteBuffer
import kotlin.jvm.JvmStatic

class OONIProbeEngineJNI {
    companion object {
        @JvmStatic
        external fun demoCheck(): String

    }

}
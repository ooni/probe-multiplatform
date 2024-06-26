package jni

import java.nio.ByteBuffer
import kotlin.jvm.JvmStatic

class OONIProbeEngineJNI {
    companion object {
        @JvmStatic
        external fun demoOne(): String

        @JvmStatic
        external fun demoTwo(): String

    }

}
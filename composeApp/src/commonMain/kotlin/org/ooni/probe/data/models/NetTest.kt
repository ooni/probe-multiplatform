package org.ooni.probe.data.models

import org.ooni.engine.models.OONINetTest
import org.ooni.engine.models.TestType

data class NetTest(
    val test: TestType,
    val inputs: List<String>? = emptyList(),
) {
    fun toOONI() = OONINetTest(test.name, inputs)

    companion object {
        fun fromOONI(netTest: OONINetTest) = NetTest(TestType.fromName(netTest.name), netTest.inputs)
    }
}

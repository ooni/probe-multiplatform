package org.ooni.probe.data.models

import kotlinx.serialization.Serializable
import org.ooni.engine.models.OONINetTest
import org.ooni.engine.models.TestType

@Serializable
data class NetTest(
    val test: TestType,
    val inputs: List<String>? = emptyList(),
    val callCheckIn: Boolean = false,
) {
    fun toOONI() = OONINetTest(test.name, inputs)

    companion object {
        fun fromOONI(netTest: OONINetTest) = NetTest(TestType.fromName(netTest.name), netTest.inputs)
    }
}

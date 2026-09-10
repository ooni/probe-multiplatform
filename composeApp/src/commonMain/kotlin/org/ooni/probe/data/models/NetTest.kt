package org.ooni.probe.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.ooni.engine.models.OONINetTest
import org.ooni.engine.models.TestType

@Serializable
data class NetTest(
    val test: TestType,
    val inputs: List<String>? = emptyList(),
    val options: JsonObject? = null,
) {
    fun toOONI() =
        OONINetTest(
            name = test.name,
            inputs = inputs,
            options = options,
        )

    companion object {
        fun fromOONI(netTest: OONINetTest) =
            NetTest(
                test = TestType.fromName(netTest.name),
                inputs = netTest.inputs,
                options = netTest.options,
            )
    }
}

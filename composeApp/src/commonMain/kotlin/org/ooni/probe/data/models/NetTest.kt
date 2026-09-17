package org.ooni.probe.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.ooni.engine.models.OONINetTest
import org.ooni.engine.models.TestType

@Serializable
data class NetTest(
    val test: TestType,
    val inputs: List<String>? = emptyList(),
    val inputsExtra: List<Map<String, String>>? = emptyList(),
    val targetsName: String? = null,
    val isBackgroundRunEnabled: Boolean = false,
    val isManualRunEnabled: Boolean = false,
    val options: JsonObject? = null,
) {
    fun toOONI() =
        OONINetTest(
            name = test.name,
            inputs = inputs?.takeIf { it.isNotEmpty() },
            inputsExtra = inputsExtra?.takeIf { it.isNotEmpty() },
            targetsName = targetsName,
            isBackgroundRunEnabled = isBackgroundRunEnabled,
            isManualRunEnabled = isManualRunEnabled,
            options = options,
        )

    companion object {
        fun fromOONI(netTest: OONINetTest) =
            NetTest(
                test = TestType.fromName(netTest.name),
                inputs = netTest.inputs.orEmpty(),
                inputsExtra = netTest.inputsExtra.orEmpty(),
                targetsName = netTest.targetsName,
                isBackgroundRunEnabled = netTest.isBackgroundRunEnabled,
                isManualRunEnabled = netTest.isManualRunEnabled,
                options = netTest.options,
            )
    }
}

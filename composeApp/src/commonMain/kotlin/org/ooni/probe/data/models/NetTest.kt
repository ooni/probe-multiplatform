package org.ooni.probe.data.models

import kotlinx.serialization.Serializable
import org.ooni.engine.models.OONINetTest
import org.ooni.engine.models.TestType

@Serializable
data class NetTest(
    val test: TestType,
    val inputs: List<String>? = emptyList(),
    val inputsExtra: List<Map<String, String>>? = emptyList(),
    val targetsName: String? = null,
    val isBackgroundRunEnabled: Boolean = test.isBackgroundRunEnabled,
    val isManualRunEnabled: Boolean = test.isManualRunEnabled,
) {
    fun toOONI() =
        OONINetTest(
            name = test.name,
            inputs = inputs,
            inputsExtra = inputsExtra,
            targetsName = targetsName,
            isBackgroundRunEnabled = isBackgroundRunEnabled,
            isManualRunEnabled = isManualRunEnabled,
        )

    companion object {
        fun fromOONI(netTest: OONINetTest): NetTest {
            val testType = TestType.fromName(
                name = netTest.name,
                isBackgroundRunEnabled = if (netTest.isBackgroundRunEnabled) true else null,
                isManualRunEnabled = if (netTest.isManualRunEnabled) true else null,
            )
            return NetTest(
                test = testType,
                inputs = netTest.inputs,
                inputsExtra = netTest.inputsExtra,
                targetsName = netTest.targetsName,
                isBackgroundRunEnabled = netTest.isBackgroundRunEnabled || testType.isBackgroundRunEnabled,
                isManualRunEnabled = netTest.isManualRunEnabled || testType.isManualRunEnabled,
            )
        }
    }
}

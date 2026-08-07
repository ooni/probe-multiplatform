package org.ooni.engine.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = TaskOriginSerializer::class)
enum class TaskOrigin(
    val value: String,
) {
    AutoRun("autorun"),
    OoniRun("ooni-run"),
    ;

    companion object {
        fun fromValue(value: String?) = entries.firstOrNull { it.value == value } ?: OoniRun
    }
}

object TaskOriginSerializer : KSerializer<TaskOrigin> {
    override val descriptor =
        PrimitiveSerialDescriptor("TaskOrigin", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: TaskOrigin,
    ) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): TaskOrigin = TaskOrigin.fromValue(decoder.decodeString())
}

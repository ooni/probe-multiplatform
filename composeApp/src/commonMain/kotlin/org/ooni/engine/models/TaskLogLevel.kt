package org.ooni.engine.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = TaskLogLevelSerializer::class)
enum class TaskLogLevel(val value: String) {
    Debug("DEBUG2"),
    Info("INFO"),
}

object TaskLogLevelSerializer : KSerializer<TaskLogLevel> {
    override val descriptor =
        PrimitiveSerialDescriptor("TaskLogLevel", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: TaskLogLevel,
    ) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): TaskLogLevel {
        val string = decoder.decodeString()
        return TaskLogLevel.entries.firstOrNull { it.value == string } ?: TaskLogLevel.Info
    }
}

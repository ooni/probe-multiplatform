package org.ooni.engine.models.serializers

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char
import kotlinx.datetime.parse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object InstantSerializer : KSerializer<Instant> {
    private val format: DateTimeFormat<DateTimeComponents> =
        DateTimeComponents.Format {
            date(
                LocalDate.Format {
                    year()
                    char('-')
                    monthNumber()
                    char('-')
                    day()
                },
            )
            char(' ')
            time(
                LocalTime.Format {
                    hour()
                    char(':')
                    minute()
                    char(':')
                    second()
                },
            )
        }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlin.time.Instant for TaskEvent Measurement", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString(), format = format)

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) {
        encoder.encodeString(value.format(format = format))
    }
}

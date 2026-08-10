package org.ooni.engine.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.times

@Serializable(with = TestTypeSerializer::class)
sealed class TestType {
    abstract val name: String
    abstract val labelResKey: String
    open val isManualRunEnabled: Boolean = true
    open val isBackgroundRunEnabled: Boolean = true
    open val iconResKey: String? = null
    open val url: String? = null

    abstract fun runtime(inputs: List<String>?): Duration

    open val preferenceKey: String
        get() = name

    data object Dash : TestType() {
        override val name: String = "dash"
        override val labelResKey: String = "Test_Dash_Fullname"
        override val isBackgroundRunEnabled: Boolean = false
        override val url: String = "https://ooni.org/nettest/dash"

        override val preferenceKey: String = "run_dash"

        override fun runtime(inputs: List<String>?) = 45.seconds
    }

    data class Experimental(
        override val name: String,
        override val isBackgroundRunEnabled: Boolean = false,
        override val isManualRunEnabled: Boolean = true,
    ) : TestType() {
        override val labelResKey: String = "Test_Experimental_Fullname"
        override val iconResKey: String = "test_experimental"

        override fun runtime(inputs: List<String>?) = 30.seconds
    }

    data object FacebookMessenger : TestType() {
        override val name: String = "facebook_messenger"
        override val labelResKey: String = "Test_FacebookMessenger_Fullname"
        override val iconResKey: String = "test_facebook_messenger"
        override val url: String = "https://ooni.org/nettest/facebook-messenger"

        override val preferenceKey: String = "test_facebook_messenger"

        override fun runtime(inputs: List<String>?) = 10.seconds
    }

    data object HttpHeaderFieldManipulation : TestType() {
        override val name: String = "http_header_field_manipulation"
        override val labelResKey: String = "Test_HTTPHeaderFieldManipulation_Fullname"
        override val url: String = "https://ooni.org/nettest/http-header-field-manipulation"

        override val preferenceKey: String = "run_http_header_field_manipulation"

        override fun runtime(inputs: List<String>?) = 5.seconds
    }

    data object HttpInvalidRequestLine : TestType() {
        override val name: String = "http_invalid_request_line"
        override val labelResKey: String = "Test_HTTPInvalidRequestLine_Fullname"
        override val url: String = "https://ooni.org/nettest/http-invalid-request-line"

        override val preferenceKey: String = "run_http_invalid_request_line"

        override fun runtime(inputs: List<String>?) = 10.seconds
    }

    data object Ndt : TestType() {
        override val name: String = "ndt"
        override val labelResKey: String = "Test_NDT_Fullname"
        override val isBackgroundRunEnabled: Boolean = false
        override val url: String = "https://ooni.org/nettest/ndt"

        override val preferenceKey: String = "run_ndt"

        override fun runtime(inputs: List<String>?) = 45.seconds
    }

    data object Psiphon : TestType() {
        override val name: String = "psiphon"
        override val labelResKey: String = "Test_Psiphon_Fullname"
        override val iconResKey: String = "test_psiphon"
        override val url: String = "https://ooni.org/nettest/psiphon"

        override val preferenceKey: String = "test_psiphon"

        override fun runtime(inputs: List<String>?) = 20.seconds
    }

    data object Signal : TestType() {
        override val name: String = "signal"
        override val labelResKey: String = "Test_Signal_Fullname"
        override val iconResKey: String = "test_signal"
        override val url: String = "https://ooni.org/nettest/signal"

        override val preferenceKey: String = "test_signal"

        override fun runtime(inputs: List<String>?) = 10.seconds
    }

    data object Telegram : TestType() {
        override val name: String = "telegram"
        override val labelResKey: String = "Test_Telegram_Fullname"
        override val iconResKey: String = "test_telegram"
        override val url: String = "https://ooni.org/nettest/telegram"

        override val preferenceKey: String = "test_telegram"

        override fun runtime(inputs: List<String>?) = 10.seconds
    }

    data object Tor : TestType() {
        override val name: String = "tor"
        override val labelResKey: String = "Test_Tor_Fullname"
        override val iconResKey: String = "test_tor"
        override val url: String = "https://ooni.org/nettest/tor"

        override val preferenceKey: String = "test_tor"

        override fun runtime(inputs: List<String>?) = 40.seconds
    }

    data object WebConnectivity : TestType() {
        override val name: String = "web_connectivity"
        override val labelResKey: String = "Test_WebConnectivity_Fullname"
        override val iconResKey: String = "test_websites"
        override val url: String = "https://ooni.org/nettest/web-connectivity"

        override val preferenceKey: String = "web_connectivity"

        override fun runtime(inputs: List<String>?) = 30.seconds + inputs.orEmpty().size.times(5.seconds)
    }

    data object Whatsapp : TestType() {
        override val name: String = "whatsapp"
        override val labelResKey: String = "Test_WhatsApp_Fullname"
        override val iconResKey: String = "test_whatsapp"
        override val url: String = "https://ooni.org/nettest/whatsapp"

        override val preferenceKey: String = "test_whatsapp"

        override fun runtime(inputs: List<String>?) = 10.seconds
    }

    companion object {
        // Lazy due to https://youtrack.jetbrains.com/issue/KT-8970/Object-is-uninitialized-null-when-accessed-from-static-context-ex.-companion-object-with-initialization-loop
        private val ALL_NAMED by lazy {
            listOf(
                Dash,
                FacebookMessenger,
                HttpHeaderFieldManipulation,
                HttpInvalidRequestLine,
                Ndt,
                Psiphon,
                Signal,
                Telegram,
                Tor,
                WebConnectivity,
                Whatsapp,
            )
        }

        fun fromName(name: String) = ALL_NAMED.firstOrNull { it.name == name } ?: Experimental(name)
    }
}

object TestTypeSerializer : KSerializer<TestType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("org.ooni.engine.models.TestType", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): TestType = TestType.fromName(decoder.decodeString())

    override fun serialize(
        encoder: Encoder,
        value: TestType,
    ) {
        encoder.encodeString(value.name)
    }
}

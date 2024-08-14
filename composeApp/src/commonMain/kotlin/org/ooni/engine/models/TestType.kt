package org.ooni.engine.models

import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Test_Dash_Fullname
import ooniprobe.composeapp.generated.resources.Test_Experimental_Fullname
import ooniprobe.composeapp.generated.resources.Test_FacebookMessenger_Fullname
import ooniprobe.composeapp.generated.resources.Test_HTTPHeaderFieldManipulation_Fullname
import ooniprobe.composeapp.generated.resources.Test_HTTPInvalidRequestLine_Fullname
import ooniprobe.composeapp.generated.resources.Test_NDT_Fullname
import ooniprobe.composeapp.generated.resources.Test_Psiphon_Fullname
import ooniprobe.composeapp.generated.resources.Test_Signal_Fullname
import ooniprobe.composeapp.generated.resources.Test_Telegram_Fullname
import ooniprobe.composeapp.generated.resources.Test_Tor_Fullname
import ooniprobe.composeapp.generated.resources.Test_WebConnectivity_Fullname
import ooniprobe.composeapp.generated.resources.Test_WhatsApp_Fullname
import ooniprobe.composeapp.generated.resources.test_experimental
import ooniprobe.composeapp.generated.resources.test_facebook_messenger
import ooniprobe.composeapp.generated.resources.test_psiphon
import ooniprobe.composeapp.generated.resources.test_signal
import ooniprobe.composeapp.generated.resources.test_telegram
import ooniprobe.composeapp.generated.resources.test_tor
import ooniprobe.composeapp.generated.resources.test_websites
import ooniprobe.composeapp.generated.resources.test_whatsapp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed class TestType {
    abstract val name: String
    abstract val labelRes: StringResource
    open val iconRes: DrawableResource? = null
    open val url: String? = null

    abstract fun runtime(inputs: List<String>?): Duration

    data object Dash : TestType() {
        override val name: String = "dash"
        override val labelRes: StringResource = Res.string.Test_Dash_Fullname
        override val url: String = "https://ooni.org/nettest/dash"

        override fun runtime(inputs: List<String>?) = 45.seconds
    }

    data class Experimental(
        override val name: String,
    ) : TestType() {
        override val labelRes: StringResource = Res.string.Test_Experimental_Fullname
        override val iconRes: DrawableResource = Res.drawable.test_experimental

        override fun runtime(inputs: List<String>?) = 30.seconds
    }

    data object FacebookMessenger : TestType() {
        override val name: String = "facebook_messenger"
        override val labelRes: StringResource = Res.string.Test_FacebookMessenger_Fullname
        override val iconRes: DrawableResource = Res.drawable.test_facebook_messenger
        override val url: String = "https://ooni.org/nettest/facebook-messenger"

        override fun runtime(inputs: List<String>?) = 10.seconds
    }

    data object HttpHeaderFieldManipulation : TestType() {
        override val name: String = "http_header_field_manipulation"
        override val labelRes: StringResource = Res.string.Test_HTTPHeaderFieldManipulation_Fullname
        override val url: String = "https://ooni.org/nettest/http-header-field-manipulation"

        override fun runtime(inputs: List<String>?) = 5.seconds
    }

    data object HttpInvalidRequestLine : TestType() {
        override val name: String = "http_invalid_request_line"
        override val labelRes: StringResource = Res.string.Test_HTTPInvalidRequestLine_Fullname
        override val url: String = "https://ooni.org/nettest/http-invalid-request-line"

        override fun runtime(inputs: List<String>?) = 10.seconds
    }

    data object Ndt : TestType() {
        override val name: String = "ndt"
        override val labelRes: StringResource = Res.string.Test_NDT_Fullname
        override val url: String = "https://ooni.org/nettest/ndt"

        override fun runtime(inputs: List<String>?) = 45.seconds
    }

    data object Psiphon : TestType() {
        override val name: String = "psiphon"
        override val labelRes: StringResource = Res.string.Test_Psiphon_Fullname
        override val iconRes: DrawableResource = Res.drawable.test_psiphon
        override val url: String = "https://ooni.org/nettest/psiphon"

        override fun runtime(inputs: List<String>?) = 20.seconds
    }

    data object Signal : TestType() {
        override val name: String = "signal"
        override val labelRes: StringResource = Res.string.Test_Signal_Fullname
        override val iconRes: DrawableResource = Res.drawable.test_signal
        override val url: String = "https://ooni.org/nettest/signal"

        override fun runtime(inputs: List<String>?) = 10.seconds
    }

    data object Telegram : TestType() {
        override val name: String = "telegram"
        override val labelRes: StringResource = Res.string.Test_Telegram_Fullname
        override val iconRes: DrawableResource = Res.drawable.test_telegram
        override val url: String = "https://ooni.org/nettest/telegram"

        override fun runtime(inputs: List<String>?) = 10.seconds
    }

    data object Tor : TestType() {
        override val name: String = "tor"
        override val labelRes: StringResource = Res.string.Test_Tor_Fullname
        override val iconRes: DrawableResource = Res.drawable.test_tor
        override val url: String = "https://ooni.org/nettest/tor"

        override fun runtime(inputs: List<String>?) = 40.seconds
    }

    data object WebConnectivity : TestType() {
        override val name: String = "web_connectivity"
        override val labelRes: StringResource = Res.string.Test_WebConnectivity_Fullname
        override val iconRes: DrawableResource = Res.drawable.test_websites
        override val url: String = "https://ooni.org/nettest/web-connectivity"

        override fun runtime(inputs: List<String>?) = 5.seconds * (inputs?.size ?: 1)
    }

    data object Whatsapp : TestType() {
        override val name: String = "whatsapp"
        override val labelRes: StringResource = Res.string.Test_WhatsApp_Fullname
        override val iconRes: DrawableResource = Res.drawable.test_whatsapp
        override val url: String = "https://ooni.org/nettest/whatsapp"

        override fun runtime(inputs: List<String>?) = 10.seconds
    }

    companion object {
        private val ALL_NAMED = listOf(
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

        fun fromName(name: String) = ALL_NAMED.firstOrNull { it.name == name } ?: Experimental(name)
    }
}

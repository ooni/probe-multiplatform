package org.ooni.engine.models

import androidx.compose.runtime.Composable
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
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

val TestType.labelRes: StringResource
    get() = when (labelResKey) {
        "Test_Dash_Fullname" -> Res.string.Test_Dash_Fullname
        "Test_Experimental_Fullname" -> Res.string.Test_Experimental_Fullname
        "Test_FacebookMessenger_Fullname" -> Res.string.Test_FacebookMessenger_Fullname
        "Test_HTTPHeaderFieldManipulation_Fullname" -> Res.string.Test_HTTPHeaderFieldManipulation_Fullname
        "Test_HTTPInvalidRequestLine_Fullname" -> Res.string.Test_HTTPInvalidRequestLine_Fullname
        "Test_NDT_Fullname" -> Res.string.Test_NDT_Fullname
        "Test_Psiphon_Fullname" -> Res.string.Test_Psiphon_Fullname
        "Test_Signal_Fullname" -> Res.string.Test_Signal_Fullname
        "Test_Telegram_Fullname" -> Res.string.Test_Telegram_Fullname
        "Test_Tor_Fullname" -> Res.string.Test_Tor_Fullname
        "Test_WebConnectivity_Fullname" -> Res.string.Test_WebConnectivity_Fullname
        "Test_WhatsApp_Fullname" -> Res.string.Test_WhatsApp_Fullname
        else -> Res.string.Test_Experimental_Fullname
    }

val TestType.iconRes: DrawableResource?
    get() = when (iconResKey) {
        "test_experimental" -> Res.drawable.test_experimental
        "test_facebook_messenger" -> Res.drawable.test_facebook_messenger
        "test_psiphon" -> Res.drawable.test_psiphon
        "test_signal" -> Res.drawable.test_signal
        "test_telegram" -> Res.drawable.test_telegram
        "test_tor" -> Res.drawable.test_tor
        "test_websites" -> Res.drawable.test_websites
        "test_whatsapp" -> Res.drawable.test_whatsapp
        else -> null
    }

val TestType.displayName: String
    @Composable
    get() = if (this is TestType.Experimental) name else stringResource(labelRes)

suspend fun TestType.displayNameSuspended() = if (this is TestType.Experimental) name else getString(labelRes)

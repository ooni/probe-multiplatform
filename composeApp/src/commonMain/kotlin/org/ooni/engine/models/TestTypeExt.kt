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
    get() = when (this) {
        is TestType.Dash -> Res.string.Test_Dash_Fullname
        is TestType.Experimental -> Res.string.Test_Experimental_Fullname
        is TestType.FacebookMessenger -> Res.string.Test_FacebookMessenger_Fullname
        is TestType.HttpHeaderFieldManipulation -> Res.string.Test_HTTPHeaderFieldManipulation_Fullname
        is TestType.HttpInvalidRequestLine -> Res.string.Test_HTTPInvalidRequestLine_Fullname
        is TestType.Ndt -> Res.string.Test_NDT_Fullname
        is TestType.Psiphon -> Res.string.Test_Psiphon_Fullname
        is TestType.Signal -> Res.string.Test_Signal_Fullname
        is TestType.Telegram -> Res.string.Test_Telegram_Fullname
        is TestType.Tor -> Res.string.Test_Tor_Fullname
        is TestType.WebConnectivity -> Res.string.Test_WebConnectivity_Fullname
        is TestType.Whatsapp -> Res.string.Test_WhatsApp_Fullname
    }

val TestType.iconRes: DrawableResource?
    get() = when (this) {
        is TestType.Experimental -> Res.drawable.test_experimental
        is TestType.FacebookMessenger -> Res.drawable.test_facebook_messenger
        is TestType.Psiphon -> Res.drawable.test_psiphon
        is TestType.Signal -> Res.drawable.test_signal
        is TestType.Telegram -> Res.drawable.test_telegram
        is TestType.Tor -> Res.drawable.test_tor
        is TestType.WebConnectivity -> Res.drawable.test_websites
        is TestType.Whatsapp -> Res.drawable.test_whatsapp
        is TestType.Dash,
        is TestType.HttpHeaderFieldManipulation,
        is TestType.HttpInvalidRequestLine,
        is TestType.Ndt,
        -> null
    }

val TestType.displayName: String
    @Composable
    get() = if (this is TestType.Experimental) name else stringResource(labelRes)

suspend fun TestType.displayNameSuspended() = if (this is TestType.Experimental) name else getString(labelRes)

package org.ooni.probe.domain

import org.ooni.engine.models.TestKeys
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.MeasurementKeysResult

fun evaluateMeasurementKeys(
    type: TestType,
    keys: TestKeys?,
): MeasurementKeysResult =
    when (type) {
        TestType.Dash,
        TestType.Ndt,
        ->
            MeasurementKeysResult(
                isFailed = keys?.failure != null,
            )

        is TestType.Experimental ->
            MeasurementKeysResult()

        TestType.FacebookMessenger ->
            MeasurementKeysResult(
                isFailed = keys?.facebookTcpBlocking == null || keys.facebookDnsBlocking == null,
                isAnomaly = keys?.facebookTcpBlocking == true || keys?.facebookDnsBlocking == true,
            )

        TestType.HttpHeaderFieldManipulation,
        TestType.HttpInvalidRequestLine,
        ->
            if (keys?.failure != null && keys.tampering == null) {
                MeasurementKeysResult(isFailed = true)
            } else {
                MeasurementKeysResult(isAnomaly = keys?.tampering?.value == true)
            }

        TestType.Psiphon ->
            MeasurementKeysResult(
                isFailed = keys == null,
                isAnomaly = keys?.failure != null,
            )

        TestType.Signal ->
            MeasurementKeysResult(
                isFailed = keys?.signalBackendStatus?.isEmpty() != false,
                isAnomaly = keys?.signalBackendStatus == TestKeys.BLOCKED_VALUE,
            )

        TestType.Telegram ->
            MeasurementKeysResult(
                isFailed = keys?.telegramHttpBlocking == null ||
                    keys.telegramTcpBlocking == null ||
                    keys.telegramWebStatus == null,
                isAnomaly = keys?.facebookTcpBlocking == true ||
                    keys?.facebookDnsBlocking == true ||
                    keys?.telegramWebStatus == TestKeys.BLOCKED_VALUE,
            )

        TestType.Tor ->
            if (keys == null) {
                MeasurementKeysResult(isFailed = true)
            } else {
                MeasurementKeysResult(
                    isAnomaly = (
                        (keys.dirPortAccessible ?: 0) <= 0 &&
                            (keys.dirPortTotal ?: 0) > 0
                    ) || (
                        (keys.obfs4Accessible ?: 0) <= 0 &&
                            (keys.obfs4Total ?: 0) > 0
                    ) || (
                        (keys.orPortDirauthAccessible ?: 0) <= 0 &&
                            (keys.orPortDirauthTotal ?: 0) > 0
                    ) || (
                        (keys.orPortAccessible ?: 0) <= 0 &&
                            (keys.orPortTotal ?: 0) > 0
                    ),
                )
            }

        TestType.WebConnectivity -> MeasurementKeysResult(
            isFailed = keys?.blocking == null,
            isAnomaly = keys?.blocking != null && keys.blocking != "false",
        )

        TestType.Whatsapp ->
            MeasurementKeysResult(
                isFailed = keys?.whatsappEndpointsStatus == null ||
                    keys.whatsappWebStatus == null ||
                    keys.registrationServerStatus == null,
                isAnomaly = keys?.whatsappEndpointsStatus == TestKeys.BLOCKED_VALUE ||
                    keys?.whatsappWebStatus == TestKeys.BLOCKED_VALUE ||
                    keys?.registrationServerStatus == TestKeys.BLOCKED_VALUE,
            )
    }

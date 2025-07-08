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
                isAnomaly = keys?.signalBackendStatus.isBlocked,
            )

        TestType.Telegram ->
            MeasurementKeysResult(
                isFailed = keys?.telegramHttpBlocking == null ||
                    keys.telegramTcpBlocking == null ||
                    keys.telegramWebStatus == null,
                isAnomaly = keys?.telegramHttpBlocking == true ||
                    keys?.telegramTcpBlocking == true ||
                    keys?.telegramWebStatus.isBlocked,
            )

        TestType.Tor ->
            if (keys == null) {
                MeasurementKeysResult(isFailed = true)
            } else {
                MeasurementKeysResult(
                    isAnomaly = (
                        (keys.dirPortAccessible ?: 0) <= 0 &&
                            (keys.dirPortTotal ?: 0) > 0
                    ) ||
                        (
                            (keys.obfs4Accessible ?: 0) <= 0 &&
                                (keys.obfs4Total ?: 0) > 0
                        ) ||
                        (
                            (keys.orPortDirauthAccessible ?: 0) <= 0 &&
                                (keys.orPortDirauthTotal ?: 0) > 0
                        ) ||
                        (
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
                isAnomaly = keys?.whatsappEndpointsStatus.isBlocked ||
                    keys?.whatsappWebStatus.isBlocked ||
                    keys?.registrationServerStatus.isBlocked,
            )
    }

private val String?.isBlocked
    get() = equals(TestKeys.BLOCKED_VALUE, ignoreCase = true)

fun extractTestKeysPropertiesToJson(testKeys: TestKeys): Map<String, Map<String, Double?>?> =
    mapOf(
        "simple" to testKeys.simple?.let { simple ->
            mapOf(
                "median_bitrate" to simple.medianBitrate,
                "upload" to simple.medianBitrate,
                "download" to simple.medianBitrate,
            ).filter { it.value != null }
        },
        "summary" to testKeys.summary?.let { summary ->
            mapOf(
                "upload" to summary.upload,
                "download" to summary.download,
                "ping" to summary.ping,
            ).filter { it.value != null }
        },
    ).filter { it.value != null }

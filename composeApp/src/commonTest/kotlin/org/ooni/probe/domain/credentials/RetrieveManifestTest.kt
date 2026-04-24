package org.ooni.probe.domain.credentials

import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.di.Dependencies
import kotlin.test.Test
import kotlin.test.assertEquals

class RetrieveManifestTest {
    @Test
    fun successful() =
        runTest {
            var preferenceSet: Any? = null
            RetrieveManifest(
                passportGet = { _, _, _ ->
                    Success(
                        PassportHttpResponse(
                            statusCode = 200,
                            version = "",
                            headersListText = emptyList(),
                            bodyText = JSON_RESPONSE,
                        ),
                    )
                },
                json = Dependencies.buildJson(),
                setPreference = { _, value -> preferenceSet = value },
                backgroundContext = coroutineContext,
            )()

            assertEquals(JSON_RESPONSE, preferenceSet)
        }

    companion object {
        private val JSON_RESPONSE = """
            {
              "manifest": {
                "nym_scope": "ooni.org/{probe_cc}/{probe_asn}",
                "submission_policy": [],
                "public_parameters": "PARAMS"
              },
              "meta": {
                "version": "1.0.0",
                "last_modification_date": "2026-03-02T18:28:59.841Z",
                "manifest_url": "https://example.org"
              }
            }
        """.trimIndent()
    }
}

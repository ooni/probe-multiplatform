package org.ooni.probe.uitesting.helpers

/**
 * Installs / reconfigures the offline [MockOonimkallBridge] on the live
 * dependency graph, replacing the real `AndroidOonimkallBridge`.
 *
 * Call this from a test's `@Before` (after `skipOnboarding()`, before
 * `start()`/triggering a run). It is scoped per-test on purpose: only the
 * tests that exercise `checkIn`/measurement runs install the mock, so other
 * instrumented tests keep the real engine and their normal behaviour
 * (articles, GeoIP, screenshots).
 *
 * `checkIn` and `startTask` are mocked here because those are the slow,
 * network-bound paths triggered when a measurement run starts (via
 * `RunDescriptors`, inside the test body — never during bootstrap).
 *
 * Usage:
 * ```
 * setupMockedEngine {
 *     httpDo = { request -> OonimkallBridge.HTTPResponse(body = ...) }
 * }
 * ```
 */
fun setupMockedEngine(configure: MockOonimkallBridge.() -> Unit = {}): MockOonimkallBridge {
    val bridge = (dependencies.engine.bridge as? MockOonimkallBridge)
        ?: MockOonimkallBridge().also { dependencies.engine.bridge = it }
    bridge.configure()
    return bridge
}

/**
 * Canned JSON fixtures shared by instrumented tests that drive descriptor
 * download/update flows through the mocked engine `httpDo`.
 */
object TestFixtures {
    const val DESCRIPTOR_URL = "https://run.test.ooni.org/v2/10460"

    /** Matched against the tail of the descriptor-fetch URL (host-agnostic). */
    const val DESCRIPTOR_LINK_PATH = "/api/v2/oonirun/links/10460"
    const val DESCRIPTOR_REVISIONS_PATH = "/api/v2/oonirun/links/10460/revisions"

    val ORIGINAL_DESCRIPTOR_JSON = """
        {
           "name":"Testing",
           "short_description":"Android instrumented tests",
           "description":"This is OONI Run Link for the Android instrumented tests",
           "author":"sergio@bloco.io",
           "nettests":[
              {
                 "test_name":"web_connectivity",
                 "inputs":[
                    "https://example.org"
                 ],
                 "options":{},
                 "backend_options":{},
                 "is_background_run_enabled_default":false,
                 "is_manual_run_enabled_default":false
              }
           ],
           "name_intl":{},
           "short_description_intl":{},
           "description_intl":{},
           "icon":"FaCube",
           "color":"#73d8ff",
           "expiration_date":"2100-12-31T00:00:00.000000Z",
           "oonirun_link_id":"10460",
           "date_created":"2024-10-09T10:53:52.000000Z",
           "date_updated":"2024-10-09T10:53:52.000000Z",
           "revision":"1",
           "is_mine":false,
           "is_expired":false
        }
    """.trimIndent()

    val UPDATED_DESCRIPTOR_JSON = """
        {
           "name":"Testing 2",
           "short_description":"Android instrumented tests",
           "description":"This is OONI Run Link for the Android instrumented tests",
           "author":"sergio@bloco.io",
           "nettests":[
              {
                 "test_name":"web_connectivity",
                 "inputs":[
                    "https://example.org"
                 ],
                 "options":{},
                 "backend_options":{},
                 "is_background_run_enabled_default":false,
                 "is_manual_run_enabled_default":false
              }
           ],
           "name_intl":{},
           "short_description_intl":{},
           "description_intl":{},
           "icon":"FaCube",
           "color":"#73d8ff",
           "expiration_date":"2100-12-31T00:00:00.000000Z",
           "oonirun_link_id":"10460",
           "date_created":"2024-10-09T10:53:52.000000Z",
           "date_updated":"2024-10-09T17:00:00.000000Z",
           "revision":"2",
           "is_mine":false,
           "is_expired":false
        }
    """.trimIndent()

    val DESCRIPTOR_REVISIONS_JSON = """
        {"revisions":["1"]}
    """.trimIndent()
}

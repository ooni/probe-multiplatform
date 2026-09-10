package org.ooni.probe.domain.descriptors

import org.ooni.probe.di.Dependencies
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParseOonirunApiErrorTest {
    private val json = Dependencies.buildJson()

    @Test
    fun parsesFieldViolationObjectDetail() {
        val error = parseOonirunApiError(422, """{"detail": {"error": "targetsName cannot be combined with inputs"}}""", json)

        assertTrue(error is OonirunApiError.InvalidNetTests)
        assertEquals("targetsName cannot be combined with inputs", error.detail)
    }

    @Test
    fun parsesDefaultPydanticValidationList() {
        val body = """{"detail": [{"loc": ["body", "name"], "msg": "String should have at least 2 characters", "type": "string_too_short"}]}"""

        val error = parseOonirunApiError(422, body, json)

        assertTrue(error is OonirunApiError.ValidationErrors)
        assertEquals(listOf("String should have at least 2 characters"), error.messages)
    }

    @Test
    fun parsesBareStringDetailAsBadRequest() {
        val error = parseOonirunApiError(
            400,
            """{"detail": "email_address must match the email address of the user who created the oonirun link"}""",
            json,
        )

        assertTrue(error is OonirunApiError.BadRequest)
        assertEquals(
            "email_address must match the email address of the user who created the oonirun link",
            error.detail,
        )
    }

    @Test
    fun parsesForbidden() {
        val error = parseOonirunApiError(403, """{"detail": "OONI Run link has expired and cannot be edited"}""", json)

        assertTrue(error is OonirunApiError.Forbidden)
    }

    @Test
    fun parsesNotFoundRegardlessOfBody() {
        val error = parseOonirunApiError(404, """{"detail": "OONI Run link not found"}""", json)

        assertEquals(OonirunApiError.NotFound, error)
    }

    @Test
    fun parsesRateLimitWithoutDetailKey() {
        val error = parseOonirunApiError(429, """{"error": "quota exceeded"}""", json)

        assertEquals(OonirunApiError.RateLimited, error)
    }

    @Test
    fun fallsBackToUnknownForUnrecognizedShape() {
        val error = parseOonirunApiError(500, "internal server error", json)

        assertTrue(error is OonirunApiError.Unknown)
        assertEquals(500, error.statusCode)
    }
}

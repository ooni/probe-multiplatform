package org.ooni.testing.factories

import org.ooni.passport.models.PassportHttpResponse

object PassportHttpResponseFactory {
    fun successful(
        statusCode: Int = 200,
        version: String = "HTTP/1.1",
        headersListText: List<List<String>> = emptyList(),
        bodyText: String? = null,
    ) = PassportHttpResponse(
        statusCode = statusCode,
        version = version,
        headersListText = headersListText,
        bodyText = bodyText,
    )

    fun error(
        statusCode: Int = 500,
        version: String = "HTTP/1.1",
        headersListText: List<List<String>> = emptyList(),
        bodyText: String? = null,
    ) = PassportHttpResponse(
        statusCode = statusCode,
        version = version,
        headersListText = headersListText,
        bodyText = bodyText,
    )
}

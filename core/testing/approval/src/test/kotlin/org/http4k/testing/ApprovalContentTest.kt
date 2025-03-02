package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.ApprovalContent.Companion.EntireHttpMessage
import org.http4k.testing.ApprovalContent.Companion.HttpTextBody
import org.http4k.testing.ApprovalContent.Companion.HttpTextMessage
import org.junit.jupiter.api.Test

class ApprovalContentTest {

    private val input = Response(OK)
        .header("some-header", "some header value")
        .body("hello")

    @Test
    fun `body only`() {
        assertThat(HttpTextBody()(input).reader().use { it.readText() }, equalTo("hello"))
    }

    @Test
    fun `body in message only`() {
        assertThat(
            HttpTextMessage { it.reversed() }(input).reader().use { it.readText() }, equalTo(
                "HTTP/1.1 200 OK\r\n" +
                    "some-header: some header value\r\n" +
                    "\r\n" +
                    "olleh"
            )
        )
    }

    @Test
    fun `body only with formatter`() {
        assertThat(HttpTextBody { it.reversed() }(input).reader().use { it.readText() }, equalTo("olleh"))
    }

    @Test
    fun `entire message`() {
        assertThat(
            EntireHttpMessage()(input).reader().use { it.readText() }, equalTo(
                ("HTTP/1.1 200 OK\r\n" +
                    "some-header: some header value\r\n" +
                    "\r\n" +
                    "hello")
            )
        )
    }
}

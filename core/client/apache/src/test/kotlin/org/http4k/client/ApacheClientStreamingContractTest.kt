package org.http4k.client

import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.server.SunHttp
import org.http4k.streaming.StreamingContract

class ApacheClientStreamingContractTest : StreamingContract() {
    override fun serverConfig() = SunHttp(0)

    override fun createClient(): HttpHandler = ApacheClient(requestBodyMode = BodyMode.Stream, responseBodyMode = BodyMode.Stream)
}

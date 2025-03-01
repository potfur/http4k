package org.http4k.mcp.testing.capabilities

import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.testing.TestMcpSender
import org.http4k.mcp.testing.nextEvent
import org.http4k.testing.TestSseClient
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class TestCompletions(private val sender: TestMcpSender, private val client: AtomicReference<TestSseClient>) :
    McpClient.Completions {
    override fun complete(
        request: CompletionRequest,
        overrideDefaultTimeout: Duration?
    ): McpResult<CompletionResponse> {
        sender(McpCompletion, McpCompletion.Request(request.ref, request.argument))
        return client.nextEvent<McpCompletion.Response, CompletionResponse>(
            { CompletionResponse(completion) }
        )
    }
}

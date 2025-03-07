package org.http4k.mcp.server.capability

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpError.Timeout
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.model.CompletionStatus.InProgress
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.messages.McpSampling
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Handles protocol traffic for sampling. Selects the best model to serve a request.
 */
class Sampling {

    private val subscriptions =
        ConcurrentHashMap<SessionId, Pair<McpEntity, (McpSampling.Request, RequestId) -> Unit>>()

    private val responseQueues = ConcurrentHashMap<RequestId, BlockingQueue<SamplingResponse>>()

    fun receive(id: RequestId, response: McpSampling.Response): CompletionStatus {
        val samplingResponse = SamplingResponse(response.model, response.role, response.content, response.stopReason)

        responseQueues[id]?.put(samplingResponse)

        return when {
            response.stopReason == null -> InProgress
            else -> {
                responseQueues.remove(id)
                Finished
            }
        }
    }

    fun sampleClient(
        entity: McpEntity,
        request: SamplingRequest,
        id: RequestId,
        fetchNextTimeout: Duration? = null
    ): Sequence<McpResult<SamplingResponse>> {
        val queue = ArrayBlockingQueue<SamplingResponse>(1000)
        responseQueues[id] = queue

        with(request) {
            subscriptions.values.filter { it.first == entity }
                .random().second.invoke(
                    McpSampling.Request(
                        messages,
                        maxTokens,
                        systemPrompt,
                        includeContext,
                        temperature,
                        stopSequences,
                        modelPreferences,
                        metadata
                    ),
                    id
                )
        }

        return sequence {
            while (true) {
                val nextMessage: SamplingResponse? = when (fetchNextTimeout) {
                    null -> queue.take()
                    else -> queue.poll(fetchNextTimeout.toMillis(), MILLISECONDS)
                }

                when (nextMessage) {
                    null -> {
                        yield(Failure(Timeout))
                        break
                    }

                    else -> {
                        yield(Success(nextMessage))

                        if (nextMessage.stopReason != null) {
                            responseQueues.remove(id)
                            break
                        }
                    }
                }
            }
        }
    }

    fun onSampleClient(sessionId: SessionId, entity: McpEntity, fn: (McpSampling.Request, RequestId) -> Unit) {
        subscriptions[sessionId] = entity to fn
    }

    fun remove(sessionId: SessionId) {
        subscriptions.remove(sessionId)
    }
}

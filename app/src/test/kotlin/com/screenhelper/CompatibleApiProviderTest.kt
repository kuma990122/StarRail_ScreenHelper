package com.screenhelper

import com.screenhelper.data.remote.provider.CompatibleApiProvider
import com.screenhelper.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class CompatibleApiProviderTest {
    private fun localUrl(server: MockWebServer, path: String) = server.url(path).newBuilder().host("127.0.0.1").build().toString()
    private val input = ModelRequest("character context", listOf(ModelTurn("user", "hi")))
    private fun event(content: String) = "data: " + JSONObject().put("choices", org.json.JSONArray().put(JSONObject().put("delta", JSONObject().put("content", content)))).toString() + "\n\n"
    @Test fun streamsWithoutLosingChunksAndUsesOneRequest() = runBlocking {
        MockWebServer().use { server ->
            server.start(java.net.InetAddress.getByName("127.0.0.1"), 0)
            val body = buildString { repeat(240) { append(event("字")) }; append(event("[[emotion:happy]]")); append("data: [DONE]\n\n") }
            server.enqueue(MockResponse().setHeader("Content-Type", "text/event-stream").setBody(body))
            val provider = CompatibleApiProvider(OkHttpClient.Builder().retryOnConnectionFailure(false).build())
            val result = provider.stream(EndpointConfig(localUrl(server, "/v1"), "mock-model", "test-only"), input, 32).onEach { delay(1) }.toList()
            assertEquals(240, result.filterIsInstance<AiEvent.Text>().sumOf { it.value.length })
            assertEquals(Emotion.HAPPY, result.filterIsInstance<AiEvent.Feeling>().single().emotion)
            val request = server.takeRequest(1, TimeUnit.SECONDS)!!
            assertEquals("/v1/chat/completions", request.path)
            assertEquals(32, JSONObject(request.body.readUtf8()).getInt("max_tokens"))
            assertEquals(1, server.requestCount)
        }
    }
    @Test fun unauthorizedDoesNotRetryOrLeakResponseBody() = runBlocking {
        MockWebServer().use { server ->
            server.start(java.net.InetAddress.getByName("127.0.0.1"), 0); server.enqueue(MockResponse().setResponseCode(401).setBody("sensitive-response"))
            val error = runCatching { CompatibleApiProvider(OkHttpClient()).stream(EndpointConfig(localUrl(server, "/"), "mock", "test-only"), input).toList() }.exceptionOrNull()
            assertNotNull(error); assertTrue(error!!.message.orEmpty().contains("401")); assertFalse(error.message.orEmpty().contains("sensitive-response")); assertEquals(1, server.requestCount)
        }
    }
    @Test fun truncatedStreamFailsInsteadOfReportingComplete() = runBlocking {
        MockWebServer().use { server ->
            server.start(java.net.InetAddress.getByName("127.0.0.1"), 0); server.enqueue(MockResponse().setBody(event("partial")))
            val error = runCatching { CompatibleApiProvider(OkHttpClient()).stream(EndpointConfig(localUrl(server, "/"), "mock", "test-only"), input).toList() }.exceptionOrNull()
            assertNotNull(error)
        }
    }
    @Test fun endpointSuffixIsNotDuplicated() {
        assertEquals("https://example.com/v1/chat/completions", CompatibleApiProvider.url("https://example.com/v1/", "chat/completions"))
        assertEquals("https://example.com/chat/completions", CompatibleApiProvider.url("https://example.com/chat/completions", "chat/completions"))
    }
    @Test(expected = IllegalArgumentException::class) fun insecurePublicEndpointRejected() { CompatibleApiProvider.url("http://example.com", "chat/completions") }
}

package dev.localai.pycharmassistant.service

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenAiClientTest {
    @Test
    fun addsNativeChatEndpointToOllamaBaseUrl() {
        assertEquals("http://localhost:11434/api/chat", OpenAiClient.ollamaEndpoint("http://localhost:11434/"))
    }

    @Test
    fun acceptsOllamaApiBaseUrl() {
        assertEquals("http://localhost:11434/api/chat", OpenAiClient.ollamaEndpoint("http://localhost:11434/api"))
    }

    @Test
    fun doesNotDuplicateOllamaChatEndpoint() {
        assertEquals("http://localhost:11434/api/chat", OpenAiClient.ollamaEndpoint("http://localhost:11434/api/chat"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOllamaUrlWithoutHttpScheme() {
        OpenAiClient.ollamaEndpoint("localhost:11434")
    }
}

package dev.localai.pycharmassistant.service

import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import dev.localai.pycharmassistant.settings.AssistantSettings
import com.google.gson.Gson
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object OpenAiClient {
    private val gson = Gson()
    private val logger = Logger.getInstance(OpenAiClient::class.java)

    fun complete(prompt: String): String {
        val settings = AssistantSettings.getInstance()
        if (settings.isOllamaEnabled()) {
            return completeWithOllama(settings, prompt)
        }

        val apiKey = settings.getApiKey()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Set an API key in Settings > Tools > OpenAI Code Assistant.")
        val timeout = settings.state.timeoutSeconds.coerceIn(1, 300)
        val baseUrl = settings.state.baseUrl.trim().trimEnd('/')
        require(baseUrl.startsWith("https://") || baseUrl.startsWith("http://")) {
            "API base URL must start with http:// or https://."
        }
        val endpoint = if (baseUrl.endsWith("/chat/completions")) {
            baseUrl
        } else {
            "$baseUrl/chat/completions"
        }
        val body = gson.toJson(
            mapOf(
                "model" to settings.state.model,
                "messages" to listOf(
                    mapOf("role" to "user", "content" to prompt)
                ),
                "temperature" to 0.2,
                "stream" to false
            )
        )
        val request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(timeout.toLong()))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeout.toLong()))
            .build()
            .send(request, HttpResponse.BodyHandlers.ofString())

        validateResponse(response.statusCode(), response.body())
        return JsonParser.parseString(response.body())
            .asJsonObject["choices"].asJsonArray[0].asJsonObject["message"]
            .asJsonObject["content"].asString
    }

    private fun completeWithOllama(settings: AssistantSettings, prompt: String): String {
        val responseTimeout = settings.state.ollamaTimeoutSeconds.coerceIn(30, 1800)
        val endpoint = ollamaEndpoint(settings.state.ollamaBaseUrl)
        val model = settings.state.ollamaModel.trim().ifBlank {
            throw IllegalStateException("Set an Ollama model in Settings > Tools > OpenAI Code Assistant.")
        }
        val body = gson.toJson(
            mapOf(
                "model" to model,
                "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
                "stream" to false,
                "options" to mapOf("temperature" to 0.2)
            )
        )
        val request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(responseTimeout.toLong()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
            .send(request, HttpResponse.BodyHandlers.ofString())

        validateResponse(response.statusCode(), response.body())
        return JsonParser.parseString(response.body())
            .asJsonObject["message"].asJsonObject["content"].asString
    }

    private fun validateResponse(statusCode: Int, responseBody: String) {
        if (statusCode !in 200..299) {
            logger.warn("AI API returned HTTP $statusCode")
            throw IllegalStateException("AI API returned HTTP $statusCode: ${responseBody.take(1000)}")
        }
    }

    internal fun ollamaEndpoint(baseUrl: String): String {
        val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
        require(normalizedBaseUrl.startsWith("https://") || normalizedBaseUrl.startsWith("http://")) {
            "Ollama base URL must start with http:// or https://."
        }
        return when {
            normalizedBaseUrl.endsWith("/api/chat") -> normalizedBaseUrl
            normalizedBaseUrl.endsWith("/api") -> "$normalizedBaseUrl/chat"
            else -> "$normalizedBaseUrl/api/chat"
        }
    }
}
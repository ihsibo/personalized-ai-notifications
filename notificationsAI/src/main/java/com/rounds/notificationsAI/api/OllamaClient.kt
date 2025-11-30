package com.ihsib.notificationsAI.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.ihsib.notificationsAI.models.NotificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Ollama Client for self-hosted AI models
 * 
 * Requirements:
 * - Ollama installed on a server: https://ollama.com
 * - Server accessible from your Android device
 * 
 * Setup:
 * 1. Install Ollama: curl -fsSL https://ollama.com/install.sh | sh
 * 2. Pull a model: ollama pull llama3.2:3b
 * 3. Start server: OLLAMA_HOST=0.0.0.0:11434 ollama serve
 * 
 * Pricing:
 * - Completely FREE - runs on your own hardware
 * - No API costs
 * - No rate limits
 * - Full privacy
 */
class OllamaClient(
    private val serverUrl: String = "http://localhost:11434",
    private val timeout: Long = 60,
    private val model: String = "llama3.2:3b"
) : AIClient {
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeout, TimeUnit.SECONDS)
        .readTimeout(timeout, TimeUnit.SECONDS)
        .writeTimeout(timeout, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    /**
     * Generate a completion using Ollama
     */
    override suspend fun generateCompletion(
        prompt: String,
        maxTokens: Int,
        temperature: Double
    ): NotificationResult = withContext(Dispatchers.IO) {
        try {
            val fullPrompt = """You are NotificationAI, an expert at creating engaging push notifications.

$prompt

Generate ONLY the notification text, no explanations."""
            
            val requestBody = JsonObject().apply {
                addProperty("model", model)
                addProperty("prompt", fullPrompt)
                addProperty("stream", false)
                add("options", JsonObject().apply {
                    addProperty("temperature", temperature)
                    addProperty("num_predict", maxTokens)
                    addProperty("top_p", 0.95)
                })
            }
            
            val json = gson.toJson(requestBody)
            val body = json.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url("$serverUrl/api/generate")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
            
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                
                if (!response.isSuccessful) {
                    return@withContext NotificationResult.Error(
                        "Ollama Error: ${response.code}. Is Ollama server running at $serverUrl?"
                    )
                }
                
                if (responseBody == null) {
                    return@withContext NotificationResult.Error("Empty response from Ollama")
                }
                
                try {
                    val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                    val text = jsonResponse.get("response")?.asString
                    
                    if (text.isNullOrEmpty()) {
                        return@withContext NotificationResult.Error("No content in Ollama response")
                    }
                    
                    NotificationResult.Success(
                        notification = text.trim(),
                        tokensUsed = null
                    )
                } catch (e: Exception) {
                    NotificationResult.Error("Failed to parse Ollama response: ${e.message}", e)
                }
            }
        } catch (e: IOException) {
            NotificationResult.Error(
                "Network error: ${e.message}. Check if Ollama server is running at $serverUrl",
                e
            )
        } catch (e: Exception) {
            NotificationResult.Error("Unexpected error: ${e.message}", e)
        }
    }
    
    /**
     * Generate multiple notification variants
     */
    override suspend fun generateVariants(
        prompt: String,
        count: Int,
        maxTokens: Int
    ): List<NotificationResult> = withContext(Dispatchers.IO) {
        (1..count).map {
            generateCompletion(prompt, maxTokens, temperature = 0.7 + (it * 0.1))
        }
    }
}


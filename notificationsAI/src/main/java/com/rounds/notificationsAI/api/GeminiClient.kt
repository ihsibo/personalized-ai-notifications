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
 * Google Gemini API Client
 * 
 * Free tier: gemini-2.5-flash and gemini-2.5-flash-lite
 * Get API key: https://makersuite.google.com/app/apikey
 * 
 * Pricing:
 * - Free tier available for gemini-2.5-flash
 * - Suitable for most production apps
 * 
 * API Documentation: https://ai.google.dev/gemini-api/docs
 */
class GeminiClient(
    private val apiKey: String,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    private val timeout: Long = 30,
    private val model: String = "gemini-2.5-flash"
) : AIClient {
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeout, TimeUnit.SECONDS)
        .readTimeout(timeout, TimeUnit.SECONDS)
        .writeTimeout(timeout, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    /**
     * Generate a completion using Google Gemini
     */
    override suspend fun generateCompletion(
        prompt: String,
        maxTokens: Int,
        temperature: Double
    ): NotificationResult = withContext(Dispatchers.IO) {
        try {
            // Build request body for Gemini API
            val requestBody = JsonObject().apply {
                add("contents", gson.toJsonTree(listOf(
                    mapOf(
                        "parts" to listOf(
                            mapOf("text" to "You are NotificationAI, an expert at creating engaging push notifications.\n\n$prompt")
                        )
                    )
                )))
                add("generationConfig", gson.toJsonTree(mapOf(
                    "temperature" to temperature,
                    "maxOutputTokens" to maxTokens,
                    "topP" to 0.95,
                    "topK" to 40
                )))
            }
            
            val json = gson.toJson(requestBody)
            val body = json.toRequestBody(mediaType)
            
            // Use v1beta API with x-goog-api-key header (not query parameter)
            val request = Request.Builder()
                .url("$baseUrl/models/$model:generateContent")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", apiKey)
                .post(body)
                .build()
            
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                
                if (!response.isSuccessful) {
                    val errorMessage = try {
                        val errorJson = gson.fromJson(responseBody, JsonObject::class.java)
                        errorJson.getAsJsonObject("error")?.get("message")?.asString
                            ?: responseBody ?: "Unknown error"
                    } catch (e: Exception) {
                        responseBody ?: "Unknown error occurred"
                    }
                    return@withContext NotificationResult.Error("Gemini API Error: $errorMessage")
                }
                
                if (responseBody == null) {
                    return@withContext NotificationResult.Error("Empty response from Gemini")
                }
                
                try {
                    val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                    
                    // Parse Gemini response format
                    val text = jsonResponse
                        .getAsJsonArray("candidates")
                        ?.get(0)?.asJsonObject
                        ?.getAsJsonObject("content")
                        ?.getAsJsonArray("parts")
                        ?.get(0)?.asJsonObject
                        ?.get("text")?.asString
                    
                    if (text.isNullOrEmpty()) {
                        return@withContext NotificationResult.Error("No content in Gemini response")
                    }
                    
                    // Gemini free tier doesn't return token count
                    NotificationResult.Success(
                        notification = text.trim(),
                        tokensUsed = null
                    )
                } catch (e: Exception) {
                    NotificationResult.Error("Failed to parse Gemini response: ${e.message}", e)
                }
            }
        } catch (e: IOException) {
            NotificationResult.Error("Network error: ${e.message}", e)
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


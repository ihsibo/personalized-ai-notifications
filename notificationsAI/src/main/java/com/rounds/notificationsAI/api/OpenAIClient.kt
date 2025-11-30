package com.ihsib.notificationsAI.api

import com.google.gson.Gson
import com.ihsib.notificationsAI.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OpenAI API Client for making requests to OpenAI's Chat Completion API
 */
class OpenAIClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val timeout: Long = 30,
    private val model: String = "gpt-3.5-turbo"
) : AIClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeout, TimeUnit.SECONDS)
        .readTimeout(timeout, TimeUnit.SECONDS)
        .writeTimeout(timeout, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    /**
     * Generate a chat completion using OpenAI's API
     */
    override suspend fun generateCompletion(
        prompt: String,
        maxTokens: Int,
        temperature: Double
    ): NotificationResult = withContext(Dispatchers.IO) {
        try {
            val requestBody = OpenAIRequest(
                model = model,
                messages = listOf(
                    Message(role = "system", content = "You are NotificationAI, an expert at creating engaging push notifications."),
                    Message(role = "user", content = prompt)
                ),
                maxTokens = maxTokens,
                temperature = temperature
            )
            
            val json = gson.toJson(requestBody)
            val body = json.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
            
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                
                if (!response.isSuccessful) {
                    val errorMessage = try {
                        val errorResponse = gson.fromJson(responseBody, OpenAIError::class.java)
                        errorResponse.error.message
                    } catch (e: Exception) {
                        responseBody ?: "Unknown error occurred"
                    }
                    return@withContext NotificationResult.Error("OpenAI API Error: $errorMessage")
                }
                
                if (responseBody == null) {
                    return@withContext NotificationResult.Error("Empty response from OpenAI")
                }
                
                try {
                    val openAIResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
                    val content = openAIResponse.choices.firstOrNull()?.message?.content?.trim()
                    
                    if (content.isNullOrEmpty()) {
                        return@withContext NotificationResult.Error("No content in response")
                    }
                    
                    NotificationResult.Success(
                        notification = content,
                        tokensUsed = openAIResponse.usage?.totalTokens
                    )
                } catch (e: Exception) {
                    NotificationResult.Error("Failed to parse response: ${e.message}", e)
                }
            }
        } catch (e: IOException) {
            NotificationResult.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            NotificationResult.Error("Unexpected error: ${e.message}", e)
        }
    }
    
    /**
     * Generate multiple notification variants for A/B testing
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


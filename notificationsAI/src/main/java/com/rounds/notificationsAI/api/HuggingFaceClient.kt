package com.ihsib.notificationsAI.api

import com.google.gson.Gson
import com.google.gson.JsonArray
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
 * Hugging Face Inference Providers API Client
 * 
 * IMPORTANT: As of July 2025, Hugging Face deprecated the old Inference API
 * for text-generation. We now use the Inference Providers API with chat-completion format.
 * 
 * Free tier: Available for supported models
 * Get API key: https://huggingface.co/settings/tokens
 * - Use a fine-grained token with "Make calls to Inference Providers" scope
 * 
 * API Endpoint: https://router.huggingface.co/v1/chat/completions
 * Format: OpenAI-style chat completion (messages array)
 * 
 * Working Models (as of 2025):
 * - meta-llama/Llama-3.1-8B-Instruct (recommended)
 * - google/gemma-2-2b-it
 * - Qwen/Qwen3-4B-Thinking-2507
 * - deepseek-ai/DeepSeek-R1
 * 
 * To verify a model supports Inference Providers:
 * 1. Visit the model's page on huggingface.co
 * 2. Look for "Deploy" or "Use this model" section
 * 3. Check if "Inference Providers" or "Inference API" tab is available
 * 
 * Documentation: https://huggingface.co/docs/inference-endpoints
 */
class HuggingFaceClient(
    private val apiKey: String,
    private val baseUrl: String = "https://router.huggingface.co/v1/chat/completions",
    private val timeout: Long = 60, // HF can be slower
    private val model: String = "meta-llama/Llama-3.1-8B-Instruct" // Recommended working model
) : AIClient {
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeout, TimeUnit.SECONDS)
        .readTimeout(timeout, TimeUnit.SECONDS)
        .writeTimeout(timeout, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    /**
     * Generate a completion using Hugging Face Inference Providers API
     * Uses OpenAI-style chat completion format
     */
    override suspend fun generateCompletion(
        prompt: String,
        maxTokens: Int,
        temperature: Double
    ): NotificationResult = withContext(Dispatchers.IO) {
        try {
            // Build request in OpenAI chat-completion format
            val messages = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", "You are NotificationAI, an expert at creating engaging push notifications. Generate ONLY the notification text, no explanations.")
                })
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", prompt)
                })
            }
            
            val requestBody = JsonObject().apply {
                addProperty("model", model)
                add("messages", messages)
                addProperty("max_tokens", maxTokens)
                addProperty("temperature", temperature)
            }
            
            val json = gson.toJson(requestBody)
            val body = json.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(baseUrl) // Use the chat/completions endpoint directly
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
            
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                
                if (!response.isSuccessful) {
                    // Check if response is HTML (error page) instead of JSON
                    val isHtml = responseBody?.contains("<!doctype html>", ignoreCase = true) == true ||
                                 responseBody?.contains("<html", ignoreCase = true) == true
                    
                    val errorMessage = if (isHtml) {
                        // HTML response usually means wrong endpoint, invalid model, or auth issue
                        when {
                            response.code == 401 || response.code == 403 -> 
                                "Invalid API key or missing permissions. Ensure your token has 'Inference Providers' scope. " +
                                "Check at https://huggingface.co/settings/tokens"
                            response.code == 404 -> 
                                "Model not found or doesn't support Inference Providers. The model '$model' may not be available. " +
                                "Try: meta-llama/Llama-3.1-8B-Instruct or google/gemma-2-2b-it"
                            response.code == 410 -> 
                                "Model permanently removed (HTTP 410). The model '$model' is no longer available. " +
                                "Use a supported model like meta-llama/Llama-3.1-8B-Instruct"
                            response.code == 503 -> 
                                "Model is loading. This can take 20-30 seconds on first request. Please try again."
                            else -> 
                                "API returned HTML error page (HTTP ${response.code}). Check your API key and model name."
                        }
                    } else {
                        // Try to parse JSON error
                        try {
                            val errorJson = gson.fromJson(responseBody, JsonObject::class.java)
                            errorJson.get("error")?.asJsonObject?.get("message")?.asString
                                ?: errorJson.get("error")?.asString
                                ?: errorJson.get("message")?.asString
                                ?: responseBody?.take(200) // Limit error message length
                                ?: "Unknown error"
                        } catch (e: Exception) {
                            responseBody?.take(200) ?: "Unknown error occurred"
                        }
                    }
                    
                    // Handle specific error codes
                    when (response.code) {
                        401, 403 -> {
                            return@withContext NotificationResult.Error(
                                "Hugging Face API Error: Authentication failed.\n\n" +
                                "Ensure:\n" +
                                "1. Your API key is valid at https://huggingface.co/settings/tokens\n" +
                                "2. You're using a fine-grained token (not a classic token)\n" +
                                "3. The token has 'Make calls to Inference Providers' scope enabled"
                            )
                        }
                        404 -> {
                            return@withContext NotificationResult.Error(
                                "Hugging Face API Error: Model '$model' not found or doesn't support Inference Providers.\n\n" +
                                "Try these working models:\n" +
                                "• meta-llama/Llama-3.1-8B-Instruct (recommended)\n" +
                                "• google/gemma-2-2b-it\n" +
                                "• Qwen/Qwen3-4B-Thinking-2507\n\n" +
                                "Update: NotificationAI.init(model=\"meta-llama/Llama-3.1-8B-Instruct\")"
                            )
                        }
                        410 -> {
                            return@withContext NotificationResult.Error(
                                "Hugging Face API Error: Model '$model' has been permanently removed (HTTP 410).\n\n" +
                                "The old Inference API for text-generation is deprecated. " +
                                "We now use Inference Providers API. Try:\n" +
                                "• meta-llama/Llama-3.1-8B-Instruct\n" +
                                "• google/gemma-2-2b-it\n\n" +
                                "Update: NotificationAI.init(model=\"meta-llama/Llama-3.1-8B-Instruct\")"
                            )
                        }
                        503 -> {
                            return@withContext NotificationResult.Error(
                                "Hugging Face model is loading. This can take 20-30 seconds on first request. Please try again."
                            )
                        }
                        else -> {
                            return@withContext NotificationResult.Error("Hugging Face API Error: $errorMessage")
                        }
                    }
                }
                
                if (responseBody == null) {
                    return@withContext NotificationResult.Error("Empty response from Hugging Face")
                }
                
                // Check if response is HTML (shouldn't happen on success, but just in case)
                if (responseBody.contains("<!doctype html>", ignoreCase = true) || 
                    responseBody.contains("<html", ignoreCase = true)) {
                    return@withContext NotificationResult.Error(
                        "Received HTML instead of JSON. This usually means:\n" +
                        "1. Invalid API key - Check at https://huggingface.co/settings/tokens\n" +
                        "2. Model '$model' not found or doesn't support Inference Providers\n" +
                        "3. API endpoint issue - ensure you're using a fine-grained token with 'Inference Providers' scope"
                    )
                }
                
                try {
                    // Parse OpenAI-style chat completion response
                    val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                    
                    // Extract text from choices[0].message.content
                    val choices = jsonResponse.getAsJsonArray("choices")
                    if (choices == null || choices.size() == 0) {
                        return@withContext NotificationResult.Error("No choices in Hugging Face response")
                    }
                    
                    val firstChoice = choices[0].asJsonObject
                    val message = firstChoice.getAsJsonObject("message")
                    val text = message.get("content")?.asString
                    
                    if (text.isNullOrEmpty()) {
                        return@withContext NotificationResult.Error("No content in Hugging Face response")
                    }
                    
                    // Extract token usage if available
                    val usage = jsonResponse.getAsJsonObject("usage")
                    val tokensUsed = usage?.get("total_tokens")?.asInt
                    
                    NotificationResult.Success(
                        notification = text.trim(),
                        tokensUsed = tokensUsed
                    )
                } catch (e: Exception) {
                    NotificationResult.Error("Failed to parse Hugging Face response: ${e.message}", e)
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


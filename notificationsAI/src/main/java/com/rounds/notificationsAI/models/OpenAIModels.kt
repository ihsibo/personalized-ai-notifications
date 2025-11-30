package com.ihsib.notificationsAI.models

import com.google.gson.annotations.SerializedName

/**
 * OpenAI API Request model
 */
data class OpenAIRequest(
    @SerializedName("model")
    val model: String = "gpt-3.5-turbo",
    
    @SerializedName("messages")
    val messages: List<Message>,
    
    @SerializedName("max_tokens")
    val maxTokens: Int = 100,
    
    @SerializedName("temperature")
    val temperature: Double = 0.7,
    
    @SerializedName("top_p")
    val topP: Double = 1.0,
    
    @SerializedName("n")
    val n: Int = 1,
    
    @SerializedName("stream")
    val stream: Boolean = false
)

data class Message(
    @SerializedName("role")
    val role: String,
    
    @SerializedName("content")
    val content: String
)

/**
 * OpenAI API Response model
 */
data class OpenAIResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("object")
    val objectType: String,
    
    @SerializedName("created")
    val created: Long,
    
    @SerializedName("model")
    val model: String,
    
    @SerializedName("choices")
    val choices: List<Choice>,
    
    @SerializedName("usage")
    val usage: Usage?
)

data class Choice(
    @SerializedName("index")
    val index: Int,
    
    @SerializedName("message")
    val message: Message,
    
    @SerializedName("finish_reason")
    val finishReason: String
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    
    @SerializedName("total_tokens")
    val totalTokens: Int
)

/**
 * Error response from OpenAI
 */
data class OpenAIError(
    @SerializedName("error")
    val error: ErrorDetails
)

data class ErrorDetails(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("param")
    val param: String?,
    
    @SerializedName("code")
    val code: String?
)


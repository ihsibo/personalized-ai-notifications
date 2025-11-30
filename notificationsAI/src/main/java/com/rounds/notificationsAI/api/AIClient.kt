package com.ihsib.notificationsAI.api

import com.ihsib.notificationsAI.models.NotificationResult

/**
 * Base interface for all AI provider clients
 */
interface AIClient {
    /**
     * Generate a completion using the AI provider
     * 
     * @param prompt The prompt to send to the AI
     * @param maxTokens Maximum tokens in the response
     * @param temperature Creativity level (0.0-1.0)
     * @return NotificationResult with success or error
     */
    suspend fun generateCompletion(
        prompt: String,
        maxTokens: Int = 100,
        temperature: Double = 0.7
    ): NotificationResult
    
    /**
     * Generate multiple variants for A/B testing
     * 
     * @param prompt The prompt to send to the AI
     * @param count Number of variants to generate
     * @param maxTokens Maximum tokens per response
     * @return List of NotificationResults
     */
    suspend fun generateVariants(
        prompt: String,
        count: Int = 3,
        maxTokens: Int = 100
    ): List<NotificationResult>
}


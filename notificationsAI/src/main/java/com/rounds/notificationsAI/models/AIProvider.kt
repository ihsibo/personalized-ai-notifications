package com.ihsib.notificationsAI.models

/**
 * Supported AI providers for notification generation
 */
enum class AIProvider(val displayName: String, val isFree: Boolean) {
    /**
     * OpenAI GPT models (paid)
     * Cost: ~$0.001 per notification
     * Quality: Excellent
     */
    OPENAI("OpenAI GPT", isFree = false),
    
    /**
     * Google Gemini (free tier available)
     * Free tier: 60 requests/minute
     * Quality: Excellent
     * Recommended for production
     */
    GEMINI("Google Gemini", isFree = true),
    
    /**
     * Hugging Face Inference API (free)
     * Free tier: Unlimited
     * Quality: Very Good
     * May be slower during peak times
     */
    HUGGING_FACE("Hugging Face", isFree = true),
    
    /**
     * Ollama (self-hosted, completely free)
     * Requires: Your own server
     * Quality: Very Good
     * Best for: Privacy, no API costs
     */
    OLLAMA("Ollama (Self-hosted)", isFree = true),
    
    /**
     * Cohere API (free trial)
     * Free tier: Trial credits
     * Quality: Very Good
     */
    COHERE("Cohere", isFree = false)
}


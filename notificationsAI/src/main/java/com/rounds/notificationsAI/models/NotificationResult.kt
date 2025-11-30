package com.ihsib.notificationsAI.models

/**
 * Result wrapper for notification generation
 */
sealed class NotificationResult {
    data class Success(
        val notification: String,
        val tokensUsed: Int? = null,
        val generatedAt: Long = System.currentTimeMillis()
    ) : NotificationResult()
    
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : NotificationResult()
}


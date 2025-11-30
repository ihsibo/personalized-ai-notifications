package com.ihsib.notificationsAI.utils

import android.content.Context
import android.content.SharedPreferences
import com.ihsib.notificationsAI.models.FrequencySettings
import java.util.concurrent.TimeUnit

/**
 * Utility class to manage notification scheduling and frequency
 * 
 * This helps implement rate limiting and frequency control to:
 * - Avoid overwhelming users
 * - Reduce API costs
 * - Comply with frequency settings
 */
class NotificationScheduler(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "notification_ai_scheduler",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_LAST_NOTIFICATION = "last_notification_time_"
        private const val KEY_NOTIFICATION_COUNT = "notification_count_"
    }
    
    /**
     * Check if a notification should be sent based on frequency settings
     * 
     * @param userId Unique identifier for the user
     * @param frequency Frequency settings
     * @return true if notification should be sent
     */
    fun shouldSendNotification(
        userId: String,
        frequency: FrequencySettings
    ): Boolean {
        val lastNotificationTime = getLastNotificationTime(userId)
        val currentTime = System.currentTimeMillis()
        val timeSinceLastNotification = currentTime - lastNotificationTime
        
        return when (frequency) {
            FrequencySettings.DAILY -> {
                timeSinceLastNotification >= TimeUnit.HOURS.toMillis(24)
            }
            FrequencySettings.WEEKLY -> {
                timeSinceLastNotification >= TimeUnit.DAYS.toMillis(7)
            }
            FrequencySettings.ADAPTIVE -> {
                // Adaptive: send more frequently if user is less active
                val count = getNotificationCount(userId)
                val minHoursBetween = if (count > 5) 48 else 24
                timeSinceLastNotification >= TimeUnit.HOURS.toMillis(minHoursBetween.toLong())
            }
            FrequencySettings.CUSTOM -> {
                // Custom logic should be implemented by the app
                true
            }
        }
    }
    
    /**
     * Record that a notification was sent
     * 
     * @param userId Unique identifier for the user
     */
    fun recordNotificationSent(userId: String) {
        prefs.edit().apply {
            putLong(KEY_LAST_NOTIFICATION + userId, System.currentTimeMillis())
            putInt(KEY_NOTIFICATION_COUNT + userId, getNotificationCount(userId) + 1)
            apply()
        }
    }
    
    /**
     * Get the last time a notification was sent to a user
     * 
     * @param userId Unique identifier for the user
     * @return Timestamp in milliseconds (0 if never sent)
     */
    fun getLastNotificationTime(userId: String): Long {
        return prefs.getLong(KEY_LAST_NOTIFICATION + userId, 0)
    }
    
    /**
     * Get the total number of notifications sent to a user
     * 
     * @param userId Unique identifier for the user
     * @return Number of notifications sent
     */
    fun getNotificationCount(userId: String): Int {
        return prefs.getInt(KEY_NOTIFICATION_COUNT + userId, 0)
    }
    
    /**
     * Get hours since last notification
     * 
     * @param userId Unique identifier for the user
     * @return Hours since last notification
     */
    fun getHoursSinceLastNotification(userId: String): Long {
        val lastTime = getLastNotificationTime(userId)
        if (lastTime == 0L) return Long.MAX_VALUE
        
        val timeDiff = System.currentTimeMillis() - lastTime
        return TimeUnit.MILLISECONDS.toHours(timeDiff)
    }
    
    /**
     * Reset notification history for a user
     * 
     * @param userId Unique identifier for the user
     */
    fun resetUserHistory(userId: String) {
        prefs.edit().apply {
            remove(KEY_LAST_NOTIFICATION + userId)
            remove(KEY_NOTIFICATION_COUNT + userId)
            apply()
        }
    }
    
    /**
     * Clear all notification history
     */
    fun clearAllHistory() {
        prefs.edit().clear().apply()
    }
}


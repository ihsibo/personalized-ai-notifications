package com.ihsib.notificationsAI.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

/**
 * Cache for storing generated notifications to reduce API calls
 * 
 * Benefits:
 * - Reduces OpenAI API costs
 * - Improves response time
 * - Works offline for cached notifications
 */
class NotificationCache(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "notification_ai_cache",
        Context.MODE_PRIVATE
    )
    
    private val gson = Gson()
    
    companion object {
        private const val CACHE_DURATION_HOURS = 24L
    }
    
    /**
     * Cached notification entry
     */
    data class CachedNotification(
        val text: String,
        val timestamp: Long,
        val tokensUsed: Int?
    )
    
    /**
     * Save a notification to cache
     * 
     * @param key Unique key for the notification (e.g., "userId_tone_packageName")
     * @param notification The notification text
     * @param tokensUsed Optional token count
     */
    fun put(key: String, notification: String, tokensUsed: Int? = null) {
        val cached = CachedNotification(
            text = notification,
            timestamp = System.currentTimeMillis(),
            tokensUsed = tokensUsed
        )
        
        val json = gson.toJson(cached)
        prefs.edit().putString(key, json).apply()
    }
    
    /**
     * Get a cached notification if it exists and is not expired
     * 
     * @param key Unique key for the notification
     * @return The cached notification text, or null if not found or expired
     */
    fun get(key: String): String? {
        val json = prefs.getString(key, null) ?: return null
        
        return try {
            val cached = gson.fromJson(json, CachedNotification::class.java)
            
            if (isExpired(cached)) {
                remove(key)
                null
            } else {
                cached.text
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get a cached notification with full details
     * 
     * @param key Unique key for the notification
     * @return The cached notification object, or null if not found or expired
     */
    fun getFull(key: String): CachedNotification? {
        val json = prefs.getString(key, null) ?: return null
        
        return try {
            val cached = gson.fromJson(json, CachedNotification::class.java)
            
            if (isExpired(cached)) {
                remove(key)
                null
            } else {
                cached
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if a cache entry exists and is valid
     * 
     * @param key Unique key for the notification
     * @return true if the entry exists and is not expired
     */
    fun has(key: String): Boolean {
        return get(key) != null
    }
    
    /**
     * Remove a cached notification
     * 
     * @param key Unique key for the notification
     */
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
    
    /**
     * Clear all cached notifications
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Clear expired cache entries
     */
    fun clearExpired() {
        val allEntries = prefs.all
        val editor = prefs.edit()
        
        allEntries.forEach { (key, value) ->
            if (value is String) {
                try {
                    val cached = gson.fromJson(value, CachedNotification::class.java)
                    if (isExpired(cached)) {
                        editor.remove(key)
                    }
                } catch (e: Exception) {
                    // Invalid entry, remove it
                    editor.remove(key)
                }
            }
        }
        
        editor.apply()
    }
    
    /**
     * Get cache statistics
     * 
     * @return Map with cache statistics
     */
    fun getStats(): Map<String, Any> {
        val allEntries = prefs.all
        var validCount = 0
        var expiredCount = 0
        var totalTokens = 0
        
        allEntries.forEach { (_, value) ->
            if (value is String) {
                try {
                    val cached = gson.fromJson(value, CachedNotification::class.java)
                    if (isExpired(cached)) {
                        expiredCount++
                    } else {
                        validCount++
                        totalTokens += cached.tokensUsed ?: 0
                    }
                } catch (e: Exception) {
                    expiredCount++
                }
            }
        }
        
        return mapOf(
            "validEntries" to validCount,
            "expiredEntries" to expiredCount,
            "totalTokensSaved" to totalTokens,
            "estimatedCostSaved" to (totalTokens * 0.000002) // rough estimate
        )
    }
    
    /**
     * Check if a cached entry is expired
     */
    private fun isExpired(cached: CachedNotification): Boolean {
        val age = System.currentTimeMillis() - cached.timestamp
        return age > TimeUnit.HOURS.toMillis(CACHE_DURATION_HOURS)
    }
    
    /**
     * Generate a cache key from notification parameters
     * 
     * @param userId User identifier
     * @param appPackageName App package name
     * @param tone Notification tone
     * @return Cache key string
     */
    fun generateKey(userId: String, appPackageName: String, tone: String): String {
        return "${userId}_${appPackageName}_${tone}".hashCode().toString()
    }
}


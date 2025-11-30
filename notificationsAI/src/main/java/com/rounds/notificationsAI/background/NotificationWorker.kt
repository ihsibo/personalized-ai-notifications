package com.ihsib.notificationsAI.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ihsib.notificationsAI.NotificationAI
import com.ihsib.notificationsAI.models.FrequencySettings
import com.ihsib.notificationsAI.models.NotificationResult
import com.ihsib.notificationsAI.models.NotificationTone
import com.ihsib.notificationsAI.utils.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker that generates and displays AI-powered notifications
 * 
 * This worker:
 * - Runs in the background even when app is closed
 * - Survives phone reboots
 * - Respects frequency settings
 * - Generates AI notification text
 * - Displays the actual notification
 */
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_APP_PACKAGE = "app_package"
        const val KEY_TONE = "tone"
        const val KEY_FREQUENCY = "frequency"
        const val KEY_CRASH_INFO = "crash_info"
        const val KEY_LOCALE = "locale"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get parameters
            val userId = inputData.getString(KEY_USER_ID) ?: "default_user"
            val appPackage = inputData.getString(KEY_APP_PACKAGE) ?: applicationContext.packageName
            val toneName = inputData.getString(KEY_TONE) ?: NotificationTone.FRIENDLY.name
            val frequencyName = inputData.getString(KEY_FREQUENCY) ?: FrequencySettings.DAILY.name
            val crashInfo = inputData.getString(KEY_CRASH_INFO)
            val locale = inputData.getString(KEY_LOCALE)
            
            val tone = try {
                NotificationTone.valueOf(toneName)
            } catch (e: Exception) {
                NotificationTone.FRIENDLY
            }
            
            val frequency = try {
                FrequencySettings.valueOf(frequencyName)
            } catch (e: Exception) {
                FrequencySettings.DAILY
            }
            
            // Check if we should send notification based on frequency
            val scheduler = NotificationScheduler(applicationContext)
            if (!scheduler.shouldSendNotification(userId, frequency)) {
                return@withContext Result.success()
            }
            
            // Check if NotificationAI is initialized
            if (!NotificationAI.isInitialized()) {
                return@withContext Result.failure()
            }
            
            // Generate AI notification
            val result = NotificationAI.generateNotificationAsync(
                appPackageName = appPackage,
                tone = tone,
                crashInfo = crashInfo,
                locale = locale
            )
            
            when (result) {
                is NotificationResult.Success -> {
                    // Display the notification
                    NotificationDisplayManager.showNotification(
                        context = applicationContext,
                        title = getNotificationTitle(tone),
                        message = result.notification,
                        userId = userId
                    )
                    
                    // Record that notification was sent
                    scheduler.recordNotificationSent(userId)
                    
                    Result.success()
                }
                is NotificationResult.Error -> {
                    // Retry on failure
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
    
    private fun getNotificationTitle(tone: NotificationTone): String {
        return when (tone) {
            NotificationTone.FRIENDLY -> "Hey there! 👋"
            NotificationTone.MOTIVATING -> "Keep going! 💪"
            NotificationTone.PLAYFUL -> "Time for fun! 🎉"
            NotificationTone.EMPATHETIC -> "We're here for you 💙"
            NotificationTone.HUMOROUS -> "Check this out! 😄"
            NotificationTone.PROFESSIONAL -> "Update Available"
        }
    }
}


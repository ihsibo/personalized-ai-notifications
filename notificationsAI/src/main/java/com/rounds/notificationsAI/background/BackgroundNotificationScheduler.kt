package com.ihsib.notificationsAI.background

import android.content.Context
import androidx.work.*
import com.ihsib.notificationsAI.models.FrequencySettings
import com.ihsib.notificationsAI.models.NotificationTone
import java.util.concurrent.TimeUnit

/**
 * Schedules background notifications using WorkManager
 * 
 * Features:
 * - Works even when app is closed
 * - Survives phone reboots
 * - Battery optimized
 * - Doze mode compatible
 */
object BackgroundNotificationScheduler {
    
    private const val WORK_TAG_PREFIX = "notification_ai_"
    private const val WORK_NAME_PREFIX = "notification_work_"
    
    /**
     * Schedule daily notifications at a specific time
     * 
     * @param context Application context
     * @param hour Hour of day (0-23)
     * @param minute Minute of hour (0-59)
     * @param userId Unique user identifier
     * @param tone Notification tone
     * @param crashInfo Optional crash information
     * @param locale Optional locale
     */
    fun scheduleDaily(
        context: Context,
        hour: Int = 10,
        minute: Int = 0,
        userId: String = "default",
        tone: NotificationTone = NotificationTone.FRIENDLY,
        crashInfo: String? = null,
        locale: String? = null
    ) {
        val initialDelay = calculateInitialDelay(hour, minute)
        
        val inputData = workDataOf(
            NotificationWorker.KEY_USER_ID to userId,
            NotificationWorker.KEY_APP_PACKAGE to context.packageName,
            NotificationWorker.KEY_TONE to tone.name,
            NotificationWorker.KEY_FREQUENCY to FrequencySettings.DAILY.name,
            NotificationWorker.KEY_CRASH_INFO to crashInfo,
            NotificationWorker.KEY_LOCALE to locale
        )
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 30,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(WORK_TAG_PREFIX + userId)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PREFIX + userId,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Schedule weekly notifications
     */
    fun scheduleWeekly(
        context: Context,
        dayOfWeek: Int = 1, // Monday
        hour: Int = 10,
        minute: Int = 0,
        userId: String = "default",
        tone: NotificationTone = NotificationTone.FRIENDLY
    ) {
        val inputData = workDataOf(
            NotificationWorker.KEY_USER_ID to userId,
            NotificationWorker.KEY_APP_PACKAGE to context.packageName,
            NotificationWorker.KEY_TONE to tone.name,
            NotificationWorker.KEY_FREQUENCY to FrequencySettings.WEEKLY.name
        )
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            repeatInterval = 7,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(WORK_TAG_PREFIX + userId)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PREFIX + userId,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Schedule immediate one-time notification (for testing)
     */
    fun scheduleImmediate(
        context: Context,
        userId: String = "default",
        tone: NotificationTone = NotificationTone.FRIENDLY
    ) {
        val inputData = workDataOf(
            NotificationWorker.KEY_USER_ID to userId,
            NotificationWorker.KEY_APP_PACKAGE to context.packageName,
            NotificationWorker.KEY_TONE to tone.name,
            NotificationWorker.KEY_FREQUENCY to FrequencySettings.CUSTOM.name
        )
        
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(inputData)
            .addTag(WORK_TAG_PREFIX + userId + "_immediate")
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
    }
    
    /**
     * Cancel scheduled notifications for a user
     */
    fun cancelScheduledNotifications(context: Context, userId: String = "default") {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PREFIX + userId)
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG_PREFIX + userId)
    }
    
    /**
     * Cancel all scheduled notifications
     */
    fun cancelAllScheduledNotifications(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
    
    /**
     * Check if notifications are scheduled for a user
     */
    fun isScheduled(context: Context, userId: String = "default"): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WORK_NAME_PREFIX + userId)
            .get()
        
        return workInfos.any { !it.state.isFinished }
    }
    
    /**
     * Reschedule notifications after device boot
     * (WorkManager handles this automatically, but this is for custom logic)
     */
    fun rescheduleAfterBoot(context: Context) {
        // WorkManager automatically reschedules work after boot
        // This method is for any additional custom logic
    }
    
    /**
     * Calculate initial delay to schedule notification at specific time
     */
    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
        }
        
        val now = System.currentTimeMillis()
        var scheduledTime = calendar.timeInMillis
        
        // If scheduled time is in the past, schedule for tomorrow
        if (scheduledTime <= now) {
            scheduledTime += TimeUnit.DAYS.toMillis(1)
        }
        
        return scheduledTime - now
    }
}


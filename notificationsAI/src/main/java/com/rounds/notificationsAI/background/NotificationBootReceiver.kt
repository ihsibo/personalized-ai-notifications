package com.ihsib.notificationsAI.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives boot completed broadcasts to reschedule notifications after phone restart
 */
class NotificationBootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("NotificationAI", "Boot completed - rescheduling notifications")
            
            // Reschedule all periodic notifications
            // WorkManager automatically handles this, but we can do custom logic here
            try {
                BackgroundNotificationScheduler.rescheduleAfterBoot(context)
            } catch (e: Exception) {
                Log.e("NotificationAI", "Failed to reschedule after boot", e)
            }
        }
    }
}


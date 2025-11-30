package com.ihsib.notificationsAI.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Handles displaying actual Android push notifications
 */
object NotificationDisplayManager {
    
    private const val CHANNEL_ID = "notification_ai_channel"
    private const val CHANNEL_NAME = "AI Notifications"
    private const val CHANNEL_DESCRIPTION = "Personalized notifications powered by AI"
    private const val NOTIFICATION_ID_BASE = 10000
    
    /**
     * Show a notification to the user
     * 
     * @param context Application context
     * @param title Notification title
     * @param message Notification message (AI-generated)
     * @param userId User identifier for tracking
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        userId: String = "default"
    ) {
        // Create notification channel (Android 8.0+)
        createNotificationChannel(context)
        
        // Get the app's launcher intent
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Default icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // Show the notification
        val notificationId = NOTIFICATION_ID_BASE + userId.hashCode()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
    
    /**
     * Create notification channel for Android 8.0+
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Cancel a notification
     */
    fun cancelNotification(context: Context, userId: String = "default") {
        val notificationId = NOTIFICATION_ID_BASE + userId.hashCode()
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
    
    /**
     * Cancel all notifications from this library
     */
    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}


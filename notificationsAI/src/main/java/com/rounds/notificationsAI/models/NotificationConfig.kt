package com.ihsib.notificationsAI.models

/**
 * Configuration for notification generation
 */
data class NotificationConfig(
    val appPackageName: String,
    val tone: NotificationTone = NotificationTone.FRIENDLY,
    val frequencySettings: FrequencySettings = FrequencySettings.DAILY,
    val maxLength: Int = 120,
    val locale: String? = null,
    val crashInfo: String? = null,
    val userSession: Map<String, String>? = null
)

/**
 * Tone options for notifications
 */
enum class NotificationTone(val value: String) {
    FRIENDLY("friendly"),
    MOTIVATING("motivating"),
    PLAYFUL("playful"),
    EMPATHETIC("empathetic"),
    HUMOROUS("humorous"),
    PROFESSIONAL("professional")
}

/**
 * Frequency settings for notifications
 */
enum class FrequencySettings(val value: String) {
    DAILY("daily"),
    WEEKLY("weekly"),
    ADAPTIVE("adaptive"),
    CUSTOM("custom")
}


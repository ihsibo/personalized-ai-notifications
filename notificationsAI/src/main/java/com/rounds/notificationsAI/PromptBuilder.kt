package com.ihsib.notificationsAI

import com.ihsib.notificationsAI.models.NotificationConfig

/**
 * Builder for creating AI prompts for notification generation
 */
class PromptBuilder {
    
    companion object {
        private const val BASE_PROMPT = """
You are NotificationAI, an advanced assistant that generates personalized push notifications for mobile apps. Your goal is to create notifications that are engaging, friendly, and encourage users to reopen the app.

The input data includes:
- userSession: {{USER_SESSION}}
- appPackageName: {{APP_PACKAGE_NAME}}
- crashInfo: {{CRASH_INFO}}
- frequencySettings: {{FREQUENCY_SETTINGS}}
- tone: {{TONE}}

Rules for generating notifications:
1. Always make the user feel welcomed back.
2. Mention context if available (like user's previous activity or app feature they interacted with).
3. Avoid sounding generic.
4. Keep it short, clear, and attention-grabbing (max {{MAX_LENGTH}} characters for push notifications).
5. Include a subtle call-to-action like "Check it now", "See your progress", "We missed you!", etc.
6. If crash info is available, acknowledge it lightly without alarming the user.
7. Tailor the style based on tone (if specified in settings).
8. If a locale is specified, generate the notification in that language: {{LOCALE}}

Return **only the notification text**. Do not include explanations or extra information.

Example outputs:
- "Hey Alex! Your latest score is waiting — jump back in and beat it!"
- "We noticed a small hiccup yesterday, but your app is ready to go now. Welcome back!"
- "Missed you! Open the app and see what's new today."

Generate 1 notification for the given input.
"""
        
        fun buildPrompt(config: NotificationConfig): String {
            var prompt = BASE_PROMPT
            
            prompt = prompt.replace("{{USER_SESSION}}", formatUserSession(config.userSession))
            prompt = prompt.replace("{{APP_PACKAGE_NAME}}", config.appPackageName)
            prompt = prompt.replace("{{CRASH_INFO}}", config.crashInfo ?: "No recent crashes")
            prompt = prompt.replace("{{FREQUENCY_SETTINGS}}", config.frequencySettings.value)
            prompt = prompt.replace("{{TONE}}", config.tone.value)
            prompt = prompt.replace("{{MAX_LENGTH}}", config.maxLength.toString())
            prompt = prompt.replace("{{LOCALE}}", config.locale ?: "not specified (use English)")
            
            return prompt
        }
        
        private fun formatUserSession(session: Map<String, String>?): String {
            return if (session.isNullOrEmpty()) {
                "No user session data available"
            } else {
                session.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            }
        }
    }
}


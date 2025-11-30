package com.ihsib.notificationsAI

import com.ihsib.notificationsAI.api.*
import com.ihsib.notificationsAI.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * NotificationAI - Main entry point for generating personalized AI-powered notifications
 * 
 * Features:
 * - Personalized notification generation using multiple AI providers
 * - Support for OpenAI, Google Gemini, Hugging Face, and Ollama
 * - Support for different tones (friendly, motivating, playful, etc.)
 * - Crash-aware notifications
 * - Multi-language support
 * - A/B testing with multiple variants
 * - Frequency management
 * 
 * Example usage:
 * ```
 * // Initialize with Google Gemini (FREE!)
 * NotificationAI.init(
 *     provider = AIProvider.GEMINI,
 *     apiKey = "your-gemini-api-key",
 *     userSession = mapOf("name" to "Alex", "level" to "5")
 * )
 * 
 * // Generate notification
 * NotificationAI.generateNotification(
 *     appPackageName = "com.example.myapp",
 *     tone = NotificationTone.PLAYFUL
 * ) { result ->
 *     when (result) {
 *         is NotificationResult.Success -> println(result.notification)
 *         is NotificationResult.Error -> println("Error: ${result.message}")
 *     }
 * }
 * ```
 */
object NotificationAI {
    
    private var apiClient: AIClient? = null
    private var defaultUserSession: Map<String, String>? = null
    private var currentProvider: AIProvider = AIProvider.GEMINI
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * Initialize the NotificationAI with an AI provider
     * 
     * @param provider AI provider to use (GEMINI, OPENAI, HUGGING_FACE, OLLAMA, COHERE)
     * @param apiKey Your API key (not needed for Ollama)
     * @param userSession Optional default user session data
     * @param model Model to use (provider-specific)
     * @param timeout Timeout in seconds for API calls (default: 30)
     * @param serverUrl Server URL (only for Ollama)
     * 
     * Example with Google Gemini (FREE):
     * ```
     * NotificationAI.init(
     *     provider = AIProvider.GEMINI,
     *     apiKey = "your-gemini-key"
     * )
     * ```
     * 
     * Example with Ollama (self-hosted):
     * ```
     * NotificationAI.init(
     *     provider = AIProvider.OLLAMA,
     *     apiKey = "", // Not needed for Ollama
     *     serverUrl = "http://your-server:11434"
     * )
     * ```
     */
    fun init(
        provider: AIProvider = AIProvider.GEMINI,
        apiKey: String = "",
        userSession: Map<String, String>? = null,
        model: String? = null,
        timeout: Long = 30,
        serverUrl: String? = null
    ) {
        // Validate API key for providers that need it
        if (provider != AIProvider.OLLAMA) {
            require(apiKey.isNotBlank()) { "${provider.displayName} requires an API key" }
        }
        
        currentProvider = provider
        defaultUserSession = userSession
        
        apiClient = when (provider) {
            AIProvider.OPENAI -> OpenAIClient(
                apiKey = apiKey,
                timeout = timeout,
                model = model ?: "gpt-3.5-turbo"
            )
            
            AIProvider.GEMINI -> GeminiClient(
                apiKey = apiKey,
                timeout = timeout,
                model = model ?: "gemini-2.5-flash"
            )
            
            AIProvider.HUGGING_FACE -> HuggingFaceClient(
                apiKey = apiKey,
                timeout = timeout,
                model = model ?: "meta-llama/Llama-3.1-8B-Instruct"
            )
            
            AIProvider.OLLAMA -> OllamaClient(
                serverUrl = serverUrl ?: "http://localhost:11434",
                timeout = timeout,
                model = model ?: "llama3.2:3b"
            )
            
            AIProvider.COHERE -> {
                // TODO: Implement Cohere client
                throw UnsupportedOperationException("Cohere provider coming soon!")
            }
        }
    }
    
    /**
     * Check if NotificationAI has been initialized
     */
    fun isInitialized(): Boolean = apiClient != null
    
    /**
     * Get the current AI provider
     */
    fun getCurrentProvider(): AIProvider = currentProvider
    
    /**
     * Update the user session data
     */
    fun updateUserSession(session: Map<String, String>) {
        defaultUserSession = session
    }
    
    /**
     * Generate a personalized notification (async with callback)
     * 
     * @param appPackageName The app's package name
     * @param crashInfo Optional crash information
     * @param tone Notification tone (default: FRIENDLY)
     * @param frequencySettings Frequency settings (default: DAILY)
     * @param maxLength Maximum notification length (default: 120)
     * @param locale Optional locale for localization
     * @param userSession Optional user session (overrides default)
     * @param callback Callback with the result
     */
    fun generateNotification(
        appPackageName: String,
        crashInfo: String? = null,
        tone: NotificationTone = NotificationTone.FRIENDLY,
        frequencySettings: FrequencySettings = FrequencySettings.DAILY,
        maxLength: Int = 120,
        locale: String? = null,
        userSession: Map<String, String>? = null,
        callback: (NotificationResult) -> Unit
    ) {
        val client = checkInitialized()
        
        scope.launch {
            try {
                val config = NotificationConfig(
                    appPackageName = appPackageName,
                    tone = tone,
                    frequencySettings = frequencySettings,
                    maxLength = maxLength,
                    locale = locale,
                    crashInfo = crashInfo,
                    userSession = userSession ?: defaultUserSession
                )
                
                val prompt = PromptBuilder.buildPrompt(config)
                val result = client.generateCompletion(
                    prompt = prompt,
                    maxTokens = 100,
                    temperature = 0.7
                )
                
                callback(result)
            } catch (e: Exception) {
                callback(NotificationResult.Error("Failed to generate notification: ${e.message}", e))
            }
        }
    }
    
    /**
     * Generate a personalized notification (suspend function for coroutines)
     * 
     * @return NotificationResult with the generated notification or error
     */
    suspend fun generateNotificationAsync(
        appPackageName: String,
        crashInfo: String? = null,
        tone: NotificationTone = NotificationTone.FRIENDLY,
        frequencySettings: FrequencySettings = FrequencySettings.DAILY,
        maxLength: Int = 120,
        locale: String? = null,
        userSession: Map<String, String>? = null
    ): NotificationResult {
        val client = checkInitialized()
        
        return try {
            val config = NotificationConfig(
                appPackageName = appPackageName,
                tone = tone,
                frequencySettings = frequencySettings,
                maxLength = maxLength,
                locale = locale,
                crashInfo = crashInfo,
                userSession = userSession ?: defaultUserSession
            )
            
            val prompt = PromptBuilder.buildPrompt(config)
            client.generateCompletion(
                prompt = prompt,
                maxTokens = 100,
                temperature = 0.7
            )
        } catch (e: Exception) {
            NotificationResult.Error("Failed to generate notification: ${e.message}", e)
        }
    }
    
    /**
     * Generate multiple notification variants for A/B testing
     * 
     * @param count Number of variants to generate (default: 3)
     * @param callback Callback with list of results
     */
    fun generateVariants(
        appPackageName: String,
        count: Int = 3,
        crashInfo: String? = null,
        tone: NotificationTone = NotificationTone.FRIENDLY,
        userSession: Map<String, String>? = null,
        callback: (List<NotificationResult>) -> Unit
    ) {
        val client = checkInitialized()
        
        scope.launch {
            try {
                val config = NotificationConfig(
                    appPackageName = appPackageName,
                    tone = tone,
                    crashInfo = crashInfo,
                    userSession = userSession ?: defaultUserSession
                )
                
                val prompt = PromptBuilder.buildPrompt(config)
                val results = client.generateVariants(
                    prompt = prompt,
                    count = count,
                    maxTokens = 100
                )
                
                callback(results)
            } catch (e: Exception) {
                callback(listOf(NotificationResult.Error("Failed to generate variants: ${e.message}", e)))
            }
        }
    }
    
    /**
     * Generate multiple notification variants (suspend function)
     */
    suspend fun generateVariantsAsync(
        appPackageName: String,
        count: Int = 3,
        crashInfo: String? = null,
        tone: NotificationTone = NotificationTone.FRIENDLY,
        userSession: Map<String, String>? = null
    ): List<NotificationResult> {
        val client = checkInitialized()
        
        return try {
            val config = NotificationConfig(
                appPackageName = appPackageName,
                tone = tone,
                crashInfo = crashInfo,
                userSession = userSession ?: defaultUserSession
            )
            
            val prompt = PromptBuilder.buildPrompt(config)
            client.generateVariants(
                prompt = prompt,
                count = count,
                maxTokens = 100
            )
        } catch (e: Exception) {
            listOf(NotificationResult.Error("Failed to generate variants: ${e.message}", e))
        }
    }
    
    private fun checkInitialized(): AIClient {
        return apiClient ?: throw IllegalStateException(
            "NotificationAI is not initialized. Call NotificationAI.init() first."
        )
    }
    
    // ==================== Background Scheduling Methods ====================
    
    /**
     * Schedule daily background notifications at a specific time
     * 
     * This will:
     * - Generate AI notifications in the background
     * - Display them automatically
     * - Work even when app is closed
     * - Survive phone reboots
     * - Respect frequency settings
     * 
     * @param context Application context
     * @param hour Hour of day (0-23, default: 10 AM)
     * @param minute Minute of hour (0-59, default: 0)
     * @param userId Unique user identifier
     * @param tone Notification tone
     * 
     * Example:
     * ```
     * NotificationAI.scheduleDaily(
     *     context = context,
     *     hour = 10,  // 10 AM
     *     minute = 0,
     *     tone = NotificationTone.FRIENDLY
     * )
     * ```
     */
    fun scheduleDaily(
        context: android.content.Context,
        hour: Int = 10,
        minute: Int = 0,
        userId: String = "default",
        tone: NotificationTone = NotificationTone.FRIENDLY,
        crashInfo: String? = null,
        locale: String? = null
    ) {
        com.ihsib.notificationsAI.background.BackgroundNotificationScheduler.scheduleDaily(
            context = context,
            hour = hour,
            minute = minute,
            userId = userId,
            tone = tone,
            crashInfo = crashInfo,
            locale = locale
        )
    }
    
    /**
     * Schedule weekly background notifications
     */
    fun scheduleWeekly(
        context: android.content.Context,
        dayOfWeek: Int = 1, // Monday
        hour: Int = 10,
        minute: Int = 0,
        userId: String = "default",
        tone: NotificationTone = NotificationTone.FRIENDLY
    ) {
        com.ihsib.notificationsAI.background.BackgroundNotificationScheduler.scheduleWeekly(
            context = context,
            dayOfWeek = dayOfWeek,
            hour = hour,
            minute = minute,
            userId = userId,
            tone = tone
        )
    }
    
    /**
     * Schedule an immediate notification (for testing)
     */
    fun scheduleImmediate(
        context: android.content.Context,
        userId: String = "default",
        tone: NotificationTone = NotificationTone.FRIENDLY
    ) {
        com.ihsib.notificationsAI.background.BackgroundNotificationScheduler.scheduleImmediate(
            context = context,
            userId = userId,
            tone = tone
        )
    }
    
    /**
     * Cancel scheduled notifications for a user
     */
    fun cancelScheduledNotifications(
        context: android.content.Context,
        userId: String = "default"
    ) {
        com.ihsib.notificationsAI.background.BackgroundNotificationScheduler.cancelScheduledNotifications(
            context = context,
            userId = userId
        )
    }
    
    /**
     * Check if notifications are scheduled for a user
     */
    fun isNotificationScheduled(
        context: android.content.Context,
        userId: String = "default"
    ): Boolean {
        return com.ihsib.notificationsAI.background.BackgroundNotificationScheduler.isScheduled(
            context = context,
            userId = userId
        )
    }
}


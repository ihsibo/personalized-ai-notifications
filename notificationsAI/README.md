# NotificationAI

<div align="center">

**🤖 AI-Powered Personalized Push Notifications for Android**

Generate engaging, personalized push notifications using OpenAI's GPT models

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-24%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)

</div>

---

## ✨ Features

- 🎯 **Personalized Notifications** - Generate context-aware notifications based on user data
- 🎨 **Multiple Tones** - Friendly, motivating, playful, empathetic, humorous, or professional
- 🌍 **Multi-Language Support** - Generate notifications in any language
- 🔄 **A/B Testing** - Generate multiple variants for optimization
- 📊 **Crash-Aware** - Acknowledge app issues gracefully in notifications
- ⚡ **Async/Coroutines** - Modern Kotlin coroutines support
- 🔐 **Secure** - API keys never leave your app

---

## 📦 Installation

### Step 1: Add the Library Module

Add the `notificationsAI` module to your project:

```gradle
// settings.gradle.kts
include(":notificationsAI")
```

### Step 2: Add Dependencies

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":notificationsAI"))
    
    // Coroutines (if not already added)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}
```

### Step 3: Sync Project

Sync your Gradle files and you're ready to go! 🚀

---

## 🚀 Quick Start

### 1. Initialize NotificationAI

Initialize the library in your Application class or Activity:

```kotlin
import com.ihsib.notificationsAI.NotificationAI

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize with your OpenAI API key
        NotificationAI.init(
            openAiApiKey = "sk-...", // Your OpenAI API key
            userSession = mapOf(
                "name" to "Alex",
                "level" to "5",
                "lastActivity" to "Completed level 4"
            )
        )
    }
}
```

### 2. Generate a Notification

```kotlin
import com.ihsib.notificationsAI.NotificationAI
import com.ihsib.notificationsAI.models.NotificationTone
import com.ihsib.notificationsAI.models.NotificationResult

NotificationAI.generateNotification(
    appPackageName = packageName,
    tone = NotificationTone.PLAYFUL
) { result ->
    when (result) {
        is NotificationResult.Success -> {
            println("Generated: ${result.notification}")
            // Show notification to user
        }
        is NotificationResult.Error -> {
            println("Error: ${result.message}")
        }
    }
}
```

### 3. Use with Coroutines

```kotlin
lifecycleScope.launch {
    val result = NotificationAI.generateNotificationAsync(
        appPackageName = packageName,
        tone = NotificationTone.MOTIVATING,
        locale = "es" // Spanish
    )
    
    when (result) {
        is NotificationResult.Success -> {
            showNotification(result.notification)
        }
        is NotificationResult.Error -> {
            Log.e("NotificationAI", result.message)
        }
    }
}
```

---

## 📚 Advanced Usage

### Multiple Tones

Choose from various tones to match your app's personality:

```kotlin
enum class NotificationTone {
    FRIENDLY,      // "Hey there! Ready for more?"
    MOTIVATING,    // "You're doing amazing! Keep it up!"
    PLAYFUL,       // "Missed you! Let's have some fun 🎉"
    EMPATHETIC,    // "We understand you've been busy..."
    HUMOROUS,      // "Your app is lonely without you 😢"
    PROFESSIONAL   // "Your account needs attention"
}
```

### A/B Testing

Generate multiple variants and test which performs best:

```kotlin
NotificationAI.generateVariants(
    appPackageName = packageName,
    count = 3,
    tone = NotificationTone.FRIENDLY
) { results ->
    results.forEach { result ->
        when (result) {
            is NotificationResult.Success -> {
                println("Variant: ${result.notification}")
            }
            is NotificationResult.Error -> {
                println("Failed: ${result.message}")
            }
        }
    }
}
```

### Crash-Aware Notifications

When your app has crashed, acknowledge it gracefully:

```kotlin
NotificationAI.generateNotification(
    appPackageName = packageName,
    crashInfo = "App crashed on startup - NullPointerException",
    tone = NotificationTone.EMPATHETIC
) { result ->
    // "We noticed a small hiccup yesterday. 
    //  Everything's fixed now - welcome back!"
}
```

### Multi-Language Support

Generate notifications in any language:

```kotlin
NotificationAI.generateNotification(
    appPackageName = packageName,
    locale = "es", // Spanish
    tone = NotificationTone.FRIENDLY
) { result ->
    // "¡Hola! Te extrañamos. Vuelve a la app y descubre lo nuevo."
}
```

### Update User Session

Update user data dynamically:

```kotlin
NotificationAI.updateUserSession(
    mapOf(
        "name" to "Sarah",
        "level" to "12",
        "lastActivity" to "Unlocked achievement",
        "points" to "1250"
    )
)
```

---

## 🎨 Configuration Options

### NotificationConfig

```kotlin
data class NotificationConfig(
    val appPackageName: String,              // Your app's package name
    val tone: NotificationTone,              // Notification tone
    val frequencySettings: FrequencySettings, // How often to send
    val maxLength: Int = 120,                // Max characters (default: 120)
    val locale: String? = null,              // Language code (e.g., "en", "es")
    val crashInfo: String? = null,           // Recent crash information
    val userSession: Map<String, String>?    // User context data
)
```

### Frequency Settings

```kotlin
enum class FrequencySettings {
    DAILY,      // Once per day
    WEEKLY,     // Once per week
    ADAPTIVE,   // Based on user activity
    CUSTOM      // Define your own logic
}
```

---

## 💡 Best Practices

### 1. Secure API Keys

**Never hardcode API keys in production!** Use BuildConfig or a secure key management solution:

```kotlin
// build.gradle.kts
android {
    buildTypes {
        release {
            buildConfigField("String", "OPENAI_API_KEY", "\"${System.getenv("OPENAI_API_KEY")}\"")
        }
    }
}

// In your code
NotificationAI.init(
    openAiApiKey = BuildConfig.OPENAI_API_KEY,
    // ...
)
```

### 2. Error Handling

Always handle errors gracefully:

```kotlin
NotificationAI.generateNotification(...) { result ->
    when (result) {
        is NotificationResult.Success -> {
            // Success path
        }
        is NotificationResult.Error -> {
            // Log error and show fallback notification
            Log.e("NotificationAI", result.message, result.exception)
            showFallbackNotification()
        }
    }
}
```

### 3. Rate Limiting

Implement your own rate limiting to avoid excessive API calls:

```kotlin
class NotificationManager {
    private val lastNotificationTime = mutableMapOf<String, Long>()
    
    fun shouldSendNotification(userId: String): Boolean {
        val lastTime = lastNotificationTime[userId] ?: 0
        val hoursSinceLastNotification = (System.currentTimeMillis() - lastTime) / (1000 * 60 * 60)
        return hoursSinceLastNotification >= 24
    }
}
```

### 4. Caching

Cache generated notifications to reduce API costs:

```kotlin
class NotificationCache {
    private val cache = mutableMapOf<String, CachedNotification>()
    
    data class CachedNotification(
        val text: String,
        val timestamp: Long
    )
    
    fun getCachedOrGenerate(key: String, generate: suspend () -> String): String {
        val cached = cache[key]
        if (cached != null && !isExpired(cached)) {
            return cached.text
        }
        // Generate new notification
    }
}
```

---

## 🔧 API Reference

### NotificationAI Object

#### init()

Initialize the library with your OpenAI API key.

```kotlin
fun init(
    openAiApiKey: String,
    userSession: Map<String, String>? = null,
    model: String = "gpt-3.5-turbo",
    timeout: Long = 30
)
```

#### generateNotification()

Generate a single notification with a callback.

```kotlin
fun generateNotification(
    appPackageName: String,
    crashInfo: String? = null,
    tone: NotificationTone = NotificationTone.FRIENDLY,
    frequencySettings: FrequencySettings = FrequencySettings.DAILY,
    maxLength: Int = 120,
    locale: String? = null,
    userSession: Map<String, String>? = null,
    callback: (NotificationResult) -> Unit
)
```

#### generateNotificationAsync()

Generate a notification using Kotlin coroutines.

```kotlin
suspend fun generateNotificationAsync(
    appPackageName: String,
    crashInfo: String? = null,
    tone: NotificationTone = NotificationTone.FRIENDLY,
    frequencySettings: FrequencySettings = FrequencySettings.DAILY,
    maxLength: Int = 120,
    locale: String? = null,
    userSession: Map<String, String>? = null
): NotificationResult
```

#### generateVariants()

Generate multiple notification variants for A/B testing.

```kotlin
fun generateVariants(
    appPackageName: String,
    count: Int = 3,
    crashInfo: String? = null,
    tone: NotificationTone = NotificationTone.FRIENDLY,
    userSession: Map<String, String>? = null,
    callback: (List<NotificationResult>) -> Unit
)
```

#### updateUserSession()

Update the user session data.

```kotlin
fun updateUserSession(session: Map<String, String>)
```

---

## 🌟 Example Outputs

Here are some examples of notifications generated by NotificationAI:

**Friendly Tone:**
> "Hey Alex! Your latest score is waiting — jump back in and beat it!"

**Motivating Tone:**
> "You've unlocked 10 achievements! Keep the momentum going 🚀"

**Playful Tone:**
> "Your app misses you! Come back and see what's new 😊"

**Empathetic Tone (with crash info):**
> "We noticed a small hiccup yesterday. Everything's fixed now - welcome back!"

**Professional Tone:**
> "Your account has 3 pending actions. Review them now."

---

## 📊 Cost Considerations

NotificationAI uses OpenAI's API, which has associated costs:

- **gpt-3.5-turbo**: ~$0.0015 per 1K tokens (recommended for most use cases)
- **gpt-4**: More expensive but higher quality

**Tips to reduce costs:**
1. Cache notifications for similar user profiles
2. Generate in batches during off-peak hours
3. Use shorter prompts when possible
4. Implement rate limiting

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 🆘 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/NotificationAI/issues)
- **Email**: support@yourcompany.com
- **OpenAI API**: [OpenAI Documentation](https://platform.openai.com/docs)

---

## 🙏 Acknowledgments

- Built with [OpenAI's GPT API](https://openai.com)
- Powered by [OkHttp](https://square.github.io/okhttp/)
- JSON parsing with [Gson](https://github.com/google/gson)

---

<div align="center">

Made with ❤️ by [Your Name]

**[Documentation](docs/) • [Examples](examples/) • [Changelog](CHANGELOG.md)**

</div>


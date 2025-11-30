# 🚀 Quick Start Guide

Get up and running with NotificationAI in 5 minutes!

## Step 1: Get Your OpenAI API Key

1. Go to [OpenAI Platform](https://platform.openai.com/api-keys)
2. Sign in or create an account
3. Click "Create new secret key"
4. Copy the key (starts with `sk-`)

**Cost**: Using GPT-3.5-turbo costs about $0.001 per notification.

---

## Step 2: Add Your API Key

Open `app/src/main/java/com/compose/personalizedainotifications/MainActivity.kt`

Find this line:
```kotlin
val apiKey = "YOUR_OPENAI_API_KEY"
```

Replace with your actual API key:
```kotlin
val apiKey = "sk-proj-abc123..." // Your actual key
```

---

## Step 3: Run the Demo App

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Click Run (▶️) or press Shift+F10
4. The app will launch on your device/emulator

---

## Step 4: Test the Library

In the demo app:
1. Click **"Generate Notification"** to create a single notification
2. Click **"Generate A/B Variants"** to create 3 variations
3. Watch the results appear in real-time!

---

## Step 5: Use in Your Own App

### 5.1 Add the Library

```kotlin
// settings.gradle.kts
include(":notificationsAI")

// app/build.gradle.kts
dependencies {
    implementation(project(":notificationsAI"))
}
```

### 5.2 Initialize

```kotlin
import com.ihsib.notificationsAI.NotificationAI
import com.ihsib.notificationsAI.models.NotificationTone

// In your Application class or Activity onCreate
NotificationAI.init(
    openAiApiKey = "sk-...",
    userSession = mapOf(
        "name" to "John",
        "level" to "5"
    )
)
```

### 5.3 Generate Notifications

```kotlin
NotificationAI.generateNotification(
    appPackageName = packageName,
    tone = NotificationTone.FRIENDLY
) { result ->
    when (result) {
        is NotificationResult.Success -> {
            // Use result.notification
            showNotification(result.notification)
        }
        is NotificationResult.Error -> {
            Log.e("NotificationAI", result.message)
        }
    }
}
```

---

## 🎨 Examples

### Different Tones

```kotlin
// Friendly and welcoming
NotificationAI.generateNotification(
    appPackageName = packageName,
    tone = NotificationTone.FRIENDLY
)

// Motivating and encouraging
NotificationAI.generateNotification(
    appPackageName = packageName,
    tone = NotificationTone.MOTIVATING
)

// Fun and playful
NotificationAI.generateNotification(
    appPackageName = packageName,
    tone = NotificationTone.PLAYFUL
)
```

### With User Context

```kotlin
// Update user session
NotificationAI.updateUserSession(
    mapOf(
        "name" to "Sarah",
        "lastActivity" to "Completed level 10",
        "achievements" to "5",
        "streak" to "7 days"
    )
)

// Generate personalized notification
NotificationAI.generateNotification(
    appPackageName = packageName,
    tone = NotificationTone.MOTIVATING
)
// Result: "Amazing Sarah! 7 days in a row! Keep that streak going 🔥"
```

### Multi-Language

```kotlin
NotificationAI.generateNotification(
    appPackageName = packageName,
    locale = "es", // Spanish
    tone = NotificationTone.FRIENDLY
)
// Result: "¡Hola! Te extrañamos. Vuelve y descubre lo nuevo."

NotificationAI.generateNotification(
    appPackageName = packageName,
    locale = "fr", // French
    tone = NotificationTone.PLAYFUL
)
// Result: "On t'a manqué ! Reviens voir ce qui est nouveau 😊"
```

### A/B Testing

```kotlin
NotificationAI.generateVariants(
    appPackageName = packageName,
    count = 3,
    tone = NotificationTone.FRIENDLY
) { results ->
    results.forEach { result ->
        if (result is NotificationResult.Success) {
            println(result.notification)
        }
    }
}
// Result: 3 different notification variations
```

### With Crash Info

```kotlin
NotificationAI.generateNotification(
    appPackageName = packageName,
    crashInfo = "App crashed on startup",
    tone = NotificationTone.EMPATHETIC
)
// Result: "We noticed a small hiccup yesterday. Everything's fixed now - welcome back!"
```

### Using Coroutines

```kotlin
lifecycleScope.launch {
    val result = NotificationAI.generateNotificationAsync(
        appPackageName = packageName,
        tone = NotificationTone.PLAYFUL
    )
    
    when (result) {
        is NotificationResult.Success -> {
            withContext(Dispatchers.Main) {
                showNotification(result.notification)
            }
        }
        is NotificationResult.Error -> {
            Log.e("Error", result.message)
        }
    }
}
```

---

## 🛠️ Utility Classes

### Rate Limiting with NotificationScheduler

```kotlin
import com.ihsib.notificationsAI.utils.NotificationScheduler
import com.ihsib.notificationsAI.models.FrequencySettings

val scheduler = NotificationScheduler(context)

// Check if should send notification
if (scheduler.shouldSendNotification("user123", FrequencySettings.DAILY)) {
    NotificationAI.generateNotification(...) { result ->
        // Record that notification was sent
        scheduler.recordNotificationSent("user123")
    }
}

// Get stats
val hoursSince = scheduler.getHoursSinceLastNotification("user123")
val totalSent = scheduler.getNotificationCount("user123")
```

### Caching with NotificationCache

```kotlin
import com.ihsib.notificationsAI.utils.NotificationCache

val cache = NotificationCache(context)

// Generate cache key
val cacheKey = cache.generateKey(
    userId = "user123",
    appPackageName = packageName,
    tone = "friendly"
)

// Check cache first
val cached = cache.get(cacheKey)
if (cached != null) {
    showNotification(cached)
} else {
    // Generate new notification
    NotificationAI.generateNotification(...) { result ->
        if (result is NotificationResult.Success) {
            // Save to cache
            cache.put(cacheKey, result.notification, result.tokensUsed)
            showNotification(result.notification)
        }
    }
}

// Get cache stats
val stats = cache.getStats()
println("Cached entries: ${stats["validEntries"]}")
println("Estimated savings: $${stats["estimatedCostSaved"]}")
```

---

## 🔐 Security Best Practice

**Never commit API keys to version control!**

### Production Setup

```kotlin
// build.gradle.kts
android {
    buildTypes {
        release {
            buildConfigField(
                "String",
                "OPENAI_API_KEY",
                "\"${System.getenv("OPENAI_API_KEY")}\""
            )
        }
    }
}

// In your code
NotificationAI.init(
    openAiApiKey = BuildConfig.OPENAI_API_KEY
)
```

### Environment Variables

```bash
# Set environment variable
export OPENAI_API_KEY="sk-..."

# Or add to ~/.zshrc or ~/.bash_profile
echo 'export OPENAI_API_KEY="sk-..."' >> ~/.zshrc
```

---

## 💰 Cost Optimization Tips

1. **Use Caching**: Cache similar notifications
   ```kotlin
   val cache = NotificationCache(context)
   ```

2. **Rate Limiting**: Don't send too frequently
   ```kotlin
   val scheduler = NotificationScheduler(context)
   ```

3. **Batch Generation**: Generate multiple at once during off-peak
   ```kotlin
   NotificationAI.generateVariants(count = 10)
   ```

4. **Use GPT-3.5**: Cheaper than GPT-4
   ```kotlin
   NotificationAI.init(model = "gpt-3.5-turbo")
   ```

---

## 📊 Sample Outputs

Here are real examples generated by NotificationAI:

**Friendly:**
> "Hey there! Your app misses you. Come back and check out what's new!"

**Motivating:**
> "You're on a roll! 5 levels completed. Keep that momentum going 🚀"

**Playful:**
> "Knock knock! Your app here. Got some cool new stuff to show you 😊"

**Empathetic:**
> "We understand you've been busy. Take a moment to relax with us."

**Humorous:**
> "Your app is getting lonely without you. Don't make it cry 😢"

**Professional:**
> "Your account has pending items requiring attention."

---

## 🆘 Troubleshooting

### "NotificationAI is not initialized"
Make sure you call `NotificationAI.init()` before generating notifications.

### "API Error: Invalid API Key"
Double-check your API key is correct and starts with `sk-`.

### "Network Error"
Check your internet connection and ensure the device can reach api.openai.com.

### "Rate Limit Exceeded"
You're making too many requests. Implement caching and rate limiting.

---

## 📚 Next Steps

- Read the [full documentation](notificationsAI/README.md)
- Explore the [demo app](app/src/main/java/com/compose/personalizedainotifications/MainActivity.kt)
- Check out [best practices](README.md#-best-practices)
- Learn about [cost optimization](README.md#-cost-considerations)

---

## 🎉 You're Ready!

Start generating amazing personalized notifications for your users!

Need help? Check out:
- 📖 [Main README](README.md)
- 📘 [Library Documentation](notificationsAI/README.md)
- 🐛 [Report Issues](https://github.com/yourusername/PersonalizedAINotifications/issues)

**Happy coding! 🚀**


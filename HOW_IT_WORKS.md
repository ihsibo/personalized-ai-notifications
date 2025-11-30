# 🔍 How NotificationAI Works

A comprehensive explanation of the NotificationAI library architecture and workflow.

---

## 📚 Table of Contents

1. [Overview](#-overview)
2. [Architecture](#-architecture)
3. [Step-by-Step Workflow](#-step-by-step-workflow)
4. [Component Details](#-component-details)
5. [Request/Response Flow](#-requestresponse-flow)
6. [Example Scenario](#-example-scenario)

---

## 🎯 Overview

**NotificationAI** is an Android library that uses OpenAI's GPT models to generate personalized push notifications. Instead of generic messages, it creates unique, context-aware notifications based on user data and app state.

### The Magic in 3 Steps

```
1. You provide: User data + App info + Desired tone
                        ↓
2. NotificationAI: Builds AI prompt → Calls OpenAI API
                        ↓
3. You receive: Personalized notification text
```

---

## 🏗️ Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      Your Android App                        │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │           NotificationAI.generateNotification()     │    │
│  │                   (Public API)                      │    │
│  └──────────────────────┬──────────────────────────────┘    │
│                         │                                    │
│  ┌──────────────────────▼──────────────────────────────┐    │
│  │              NotificationAI Object                   │    │
│  │  - Manages initialization                            │    │
│  │  - Stores API key & user session                     │    │
│  │  - Coordinates components                            │    │
│  └──────────────────────┬──────────────────────────────┘    │
│                         │                                    │
│           ┌─────────────┴─────────────┐                     │
│           │                           │                      │
│  ┌────────▼─────────┐      ┌─────────▼──────────┐          │
│  │  PromptBuilder   │      │   OpenAIClient     │          │
│  │                  │      │                    │          │
│  │ - Builds prompt  │      │ - HTTP requests    │          │
│  │ - Substitutes    │      │ - API calls        │          │
│  │   variables      │      │ - Error handling   │          │
│  └──────────────────┘      └─────────┬──────────┘          │
│                                       │                      │
└───────────────────────────────────────┼──────────────────────┘
                                        │
                                        │ HTTPS
                                        ▼
                        ┌───────────────────────────┐
                        │   OpenAI API              │
                        │   (api.openai.com)        │
                        │                           │
                        │   GPT-3.5-turbo           │
                        │   GPT-4, etc.             │
                        └───────────────────────────┘
```

### Core Components

| Component | Responsibility | Technology |
|-----------|---------------|------------|
| **NotificationAI** | Main API & orchestration | Kotlin Object (Singleton) |
| **PromptBuilder** | Generates AI prompts | String templating |
| **OpenAIClient** | HTTP communication | OkHttp 4.12 |
| **Models** | Data structures | Kotlin data classes |
| **Utilities** | Caching & scheduling | SharedPreferences |

---

## 🔄 Step-by-Step Workflow

### Phase 1: Initialization

```kotlin
NotificationAI.init(
    openAiApiKey = "sk-...",
    userSession = mapOf("name" to "El Cipher", "age" to "22")
)
```

**What happens:**
1. ✅ Validates API key is not blank
2. ✅ Creates `OpenAIClient` instance with the API key
3. ✅ Stores default user session for future use
4. ✅ Sets up coroutine scope for async operations

**Result:** Library is ready to generate notifications

---

### Phase 2: Generate Notification Request

```kotlin
NotificationAI.generateNotification(
    appPackageName = "com.example.app",
    tone = NotificationTone.FRIENDLY,
    crashInfo = null
) { result ->
    // Handle result
}
```

**What happens:**

#### Step 2.1: Create Configuration
```kotlin
val config = NotificationConfig(
    appPackageName = "com.example.app",
    tone = NotificationTone.FRIENDLY,
    frequencySettings = FrequencySettings.DAILY,
    maxLength = 120,
    locale = null,
    crashInfo = null,
    userSession = mapOf("name" to "El Cipher", "age" to "22")
)
```

#### Step 2.2: Build AI Prompt
`PromptBuilder.buildPrompt(config)` creates:

```
You are NotificationAI, an advanced assistant...

The input data includes:
- userSession: name: El Cipher, age: 22
- appPackageName: com.example.app
- crashInfo: No recent crashes
- frequencySettings: daily
- tone: friendly

Rules for generating notifications:
1. Always make the user feel welcomed back.
2. Mention context if available...
[... more rules ...]

Generate 1 notification for the given input.
```

#### Step 2.3: Prepare API Request
`OpenAIClient` creates JSON payload:

```json
{
  "model": "gpt-3.5-turbo",
  "messages": [
    {
      "role": "system",
      "content": "You are NotificationAI, an expert at creating engaging push notifications."
    },
    {
      "role": "user",
      "content": "[The full prompt from Step 2.2]"
    }
  ],
  "max_tokens": 100,
  "temperature": 0.7
}
```

---

### Phase 3: API Communication

```
Your App  →  OpenAIClient  →  OpenAI API
                ↓
         [OkHttp Request]
                ↓
    POST https://api.openai.com/v1/chat/completions
    Header: Authorization: Bearer sk-...
    Header: Content-Type: application/json
    Body: [JSON from Phase 2.3]
                ↓
         [Wait 1-3 seconds]
                ↓
         [OpenAI Response]
```

**OpenAI processes:**
1. 🤖 Analyzes the prompt
2. 🧠 Uses GPT model to understand context
3. 📝 Generates personalized notification
4. 📤 Returns JSON response

---

### Phase 4: Response Processing

**OpenAI returns:**
```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1699999999,
  "model": "gpt-3.5-turbo",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hey El Cipher! We've got something special waiting for you. Open the app now! 🎉"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 250,
    "completion_tokens": 20,
    "total_tokens": 270
  }
}
```

**OpenAIClient parses response:**
```kotlin
val response = gson.fromJson(responseBody, OpenAIResponse::class.java)
val notificationText = response.choices[0].message.content.trim()
val tokensUsed = response.usage?.totalTokens

return NotificationResult.Success(
    notification = notificationText,
    tokensUsed = tokensUsed
)
```

---

### Phase 5: Return to Your App

```kotlin
{ result ->
    when (result) {
        is NotificationResult.Success -> {
            // ✅ Got: "Hey El Cipher! We've got something special..."
            showNotification(result.notification)
        }
        is NotificationResult.Error -> {
            // ❌ Something went wrong
            Log.e("Error", result.message)
        }
    }
}
```

---

## 🔧 Component Details

### 1. NotificationAI (Main API)

**Role:** Orchestrator and public interface

```kotlin
object NotificationAI {
    private var apiClient: OpenAIClient?
    private var defaultUserSession: Map<String, String>?
    
    fun init() { ... }
    fun generateNotification() { ... }
    fun generateNotificationAsync() { ... }
    fun generateVariants() { ... }
}
```

**Responsibilities:**
- ✅ Validate initialization
- ✅ Manage API key securely
- ✅ Store default user session
- ✅ Coordinate between PromptBuilder and OpenAIClient
- ✅ Handle async operations with coroutines
- ✅ Provide callback and suspend function APIs

---

### 2. PromptBuilder

**Role:** AI prompt generator

```kotlin
class PromptBuilder {
    companion object {
        private const val BASE_PROMPT = """..."""
        
        fun buildPrompt(config: NotificationConfig): String {
            // Replace {{PLACEHOLDERS}} with actual values
        }
    }
}
```

**Template Variables:**
- `{{USER_SESSION}}` → User data (name, age, activity, etc.)
- `{{APP_PACKAGE_NAME}}` → Your app identifier
- `{{CRASH_INFO}}` → Recent crash data (if any)
- `{{TONE}}` → Desired notification style
- `{{FREQUENCY_SETTINGS}}` → How often to notify
- `{{MAX_LENGTH}}` → Character limit
- `{{LOCALE}}` → Language preference

---

### 3. OpenAIClient

**Role:** HTTP communication with OpenAI API

```kotlin
class OpenAIClient(
    private val apiKey: String,
    private val timeout: Long = 30
) {
    private val client: OkHttpClient
    private val gson = Gson()
    
    suspend fun generateCompletion(): NotificationResult
}
```

**Key Features:**
- ✅ HTTPS-only communication
- ✅ 30-second timeout (configurable)
- ✅ Automatic JSON serialization/deserialization
- ✅ Comprehensive error handling
- ✅ Connection pooling with OkHttp
- ✅ Coroutine-based async operations

**Error Handling:**
- Network errors (no internet)
- API errors (invalid key, rate limits)
- Parsing errors (malformed response)
- Timeout errors

---

### 4. Data Models

#### NotificationConfig
```kotlin
data class NotificationConfig(
    val appPackageName: String,
    val tone: NotificationTone,
    val frequencySettings: FrequencySettings,
    val maxLength: Int = 120,
    val locale: String? = null,
    val crashInfo: String? = null,
    val userSession: Map<String, String>? = null
)
```

#### NotificationResult (Sealed Class)
```kotlin
sealed class NotificationResult {
    data class Success(
        val notification: String,
        val tokensUsed: Int?,
        val generatedAt: Long
    ) : NotificationResult()
    
    data class Error(
        val message: String,
        val exception: Throwable?
    ) : NotificationResult()
}
```

---

## 📊 Request/Response Flow

### Complete Timeline

```
Time    Action                              Component           Details
─────────────────────────────────────────────────────────────────────────
0ms     User calls generateNotification()   Your App           
        ↓
5ms     Validate initialization            NotificationAI      Check API key exists
        ↓
10ms    Create configuration               NotificationAI      Build NotificationConfig
        ↓
15ms    Build AI prompt                    PromptBuilder       Replace template variables
        ↓
20ms    Launch coroutine                   NotificationAI      Start async operation
        ↓
25ms    Create JSON payload                OpenAIClient        Serialize request
        ↓
30ms    Send HTTP request                  OkHttp              POST to api.openai.com
        ↓
        [Network Travel Time: ~50-100ms]
        ↓
        [OpenAI Processing: ~1000-2000ms]
        ↓
        [Response Travel Time: ~50-100ms]
        ↓
1200ms  Receive HTTP response              OkHttp              Status 200 + JSON body
        ↓
1205ms  Parse JSON                         OpenAIClient        Gson deserialization
        ↓
1210ms  Extract notification text          OpenAIClient        Get choices[0].message.content
        ↓
1215ms  Create Success result              OpenAIClient        NotificationResult.Success
        ↓
1220ms  Invoke callback                    NotificationAI      Pass result to your app
        ↓
1225ms  Display notification               Your App            Show to user
```

**Total Time:** ~1.2 seconds (typical)

---

## 🎬 Example Scenario

### Real-World Example: Instagram

Let's walk through a complete example:

#### Your Code:
```kotlin
// 1. Initialize (once in Application class)
NotificationAI.init(
    openAiApiKey = "sk-proj-abc123...",
    userSession = mapOf(
        "name" to "El Cipher",
        "age" to "22",
        "lastActivity" to "Created romantic frame",
        "photosEdited" to "15",
        "favoriteStyle" to "Vintage"
    )
)

// 2. Generate notification (in background service)
NotificationAI.generateNotification(
    appPackageName = "com.instagram.android",
    tone = NotificationTone.PLAYFUL,
    locale = "en"
) { result ->
    when (result) {
        is NotificationResult.Success -> {
            sendPushNotification(result.notification)
        }
        is NotificationResult.Error -> {
            Log.e("NotificationAI", result.message)
        }
    }
}
```

#### What Happens Behind the Scenes:

**1. Configuration Created:**
```kotlin
NotificationConfig(
    appPackageName = "com.instagram.android",
    tone = PLAYFUL,
    userSession = {name: El Cipher, age: 22, lastActivity: Created romantic frame, ...}
)
```

**2. Prompt Built:**
```
You are NotificationAI...

The input data includes:
- userSession: name: El Cipher, age: 22, lastActivity: Created romantic frame, 
               photosEdited: 15, favoriteStyle: Vintage
- appPackageName: com.instagram.android
- tone: playful

[Rules...]

Generate 1 notification.
```

**3. API Request Sent:**
```http
POST /v1/chat/completions HTTP/1.1
Host: api.openai.com
Authorization: Bearer sk-proj-abc123...
Content-Type: application/json

{
  "model": "gpt-3.5-turbo",
  "messages": [...prompt...],
  "max_tokens": 100,
  "temperature": 0.7
}
```

**4. OpenAI Responds:**
```json
{
  "choices": [{
    "message": {
      "content": "El Cipher! Ready for frame #16? Your vintage style is 🔥 Create something amazing today! 📸"
    }
  }],
  "usage": {
    "total_tokens": 285
  }
}
```

**5. Your App Receives:**
```kotlin
NotificationResult.Success(
    notification = "El Cipher! Ready for frame #16? Your vintage style is 🔥...",
    tokensUsed = 285,
    generatedAt = 1700000000
)
```

**6. User Sees:**
```
┌─────────────────────────────────────────┐
│  📱 Instagram                           │
├─────────────────────────────────────────┤
│  El Cipher! Ready for frame #16? Your   │
│  vintage style is 🔥 Create something   │
│  amazing today! 📸                      │
│                                         │
│  [View] [Dismiss]                       │
└─────────────────────────────────────────┘
```

---

## 🎯 Key Takeaways

### How It Works in Simple Terms:

1. **You provide context** → User data + app info + preferences
2. **NotificationAI builds an AI prompt** → Fills in template with your data
3. **Calls OpenAI API** → Sends prompt to GPT model
4. **GPT generates text** → Creates personalized notification
5. **You receive result** → Ready-to-use notification text

### Why It's Powerful:

✅ **Context-Aware**: Uses user data to personalize  
✅ **Intelligent**: GPT understands app type from package name  
✅ **Flexible**: 6 tones, multiple languages  
✅ **Easy to Use**: 2 lines of code to generate  
✅ **Production-Ready**: Error handling, caching, rate limiting  

### The Magic:

Instead of writing:
```kotlin
"Hello user, come back to our app!"  // Generic 😴
```

You get:
```kotlin
"El Cipher! Ready for frame #16? Your vintage style is 🔥..."  // Personalized 🎉
```

---

## 📈 Performance & Optimization

### Response Times:
- **Without cache**: 1-3 seconds (API call)
- **With cache**: < 1ms (instant)

### Cost:
- **Per notification**: ~$0.001 (GPT-3.5-turbo)
- **With 50% cache hit**: ~$0.0005 average

### Optimization Features:
- ✅ **NotificationCache**: Reduces duplicate API calls
- ✅ **NotificationScheduler**: Prevents over-messaging
- ✅ **Connection pooling**: Reuses HTTP connections
- ✅ **Coroutines**: Non-blocking async operations

---

## 🔒 Security

### How API Keys Are Handled:

1. **Never hardcoded** → Use BuildConfig or environment variables
2. **Stored in memory only** → Not persisted to disk
3. **Transmitted securely** → HTTPS with certificate pinning
4. **Not logged** → Excluded from crash reports

### Network Security:

```kotlin
val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

// All requests go through HTTPS only
POST https://api.openai.com/...
```

---

## 🎓 Summary

**NotificationAI works by:**

1. Taking your user data and app context
2. Building an intelligent AI prompt with that information
3. Sending the prompt to OpenAI's GPT model via HTTPS
4. Receiving a personalized notification text
5. Returning it to your app in < 2 seconds

**It's that simple!** The complexity is hidden behind a clean API, making it easy to generate engaging, personalized notifications for your users.

---

<div align="center">

**Questions?** Check out [README.md](README.md) or [QUICKSTART.md](QUICKSTART.md)

**Want to dive deeper?** See [notificationsAI/README.md](notificationsAI/README.md) for API details

</div>


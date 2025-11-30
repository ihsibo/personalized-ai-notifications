package com.compose.personalizedainotifications

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ihsib.notificationsAI.NotificationAI
import com.ihsib.notificationsAI.models.AIProvider
import com.ihsib.notificationsAI.models.NotificationResult
import com.ihsib.notificationsAI.models.NotificationTone

/**
 * MainActivity - Demo app showcasing NotificationAI library usage
 * 
 * This example demonstrates:
 * 1. Initializing NotificationAI with multiple AI providers (OpenAI, Gemini, HuggingFace)
 * 2. Generating personalized notifications
 * 3. A/B testing with multiple variants
 * 4. Using different tones and user sessions
 * 
 * IMPORTANT: Add your API key for the provider you want to use
 * - Google Gemini (FREE): https://makersuite.google.com/app/apikey
 * - OpenAI: https://platform.openai.com/api-keys
 * - Hugging Face (FREE): https://huggingface.co/settings/tokens
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var btnGenerate: Button
    private lateinit var btnGenerateVariants: Button
    private lateinit var btnCopy: Button
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Setup edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize views
        statusText = findViewById(R.id.statusText)
        resultText = findViewById(R.id.resultText)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnGenerateVariants = findViewById(R.id.btnGenerateVariants)
        btnCopy = findViewById(R.id.btnCopy)
        progressBar = findViewById(R.id.progressBar)
        
        // Initialize NotificationAI
        initializeNotificationAI()
        
        // Setup click listeners
        btnGenerate.setOnClickListener {
            generateSingleNotification()
        }
        
        btnGenerateVariants.setOnClickListener {
            generateNotificationVariants()
        }
        
        btnCopy.setOnClickListener {
            copyToClipboard()
        }
        
        // Schedule background notifications (optional - commented out by default)
        // Uncomment to enable automatic daily notifications
        // scheduleBackgroundNotifications()
    }
    
    /**
     * Initialize the NotificationAI library
     * 
     * Choose your AI provider and add the API key:
     * 
     * Option 1: Google Gemini (FREE - Recommended)
     * - Get key: https://makersuite.google.com/app/apikey
     * - Free tier: 60 requests/minute
     * 
     * Option 2: Hugging Face (FREE)
     * - Get key: https://huggingface.co/settings/tokens
     * - Free tier: Unlimited (may be slower)
     * 
     * Option 3: OpenAI (Paid)
     * - Get key: https://platform.openai.com/api-keys
     * - Cost: ~$0.001 per notification
     */
    private fun initializeNotificationAI() {
        try {
            // ═══════════════════════════════════════════════════════════
            // CHOOSE YOUR PROVIDER AND ADD YOUR API KEY BELOW
            // ═══════════════════════════════════════════════════════════
            
            // OPTION 1: Google Gemini (FREE - Recommended) ⭐
//            val provider = AIProvider.GEMINI
//            val apiKey = "YOUR_GEMINI_API_KEY"  // Get from: https://makersuite.google.com/app/apikey
            
            // OPTION 2: Hugging Face (FREE)
             val provider = AIProvider.HUGGING_FACE
             val apiKey = "YOUR_HF_API_KEY"  // Get from: https://huggingface.co/settings/tokens
            
            // OPTION 3: OpenAI (Paid)
            // val provider = AIProvider.OPENAI
            // val apiKey = "YOUR_OPENAI_API_KEY"  // Get from: https://platform.openai.com/api-keys
            
            // OPTION 4: Ollama (Self-hosted, FREE)
            // val provider = AIProvider.OLLAMA
            // val apiKey = ""  // Not needed
            // val serverUrl = "http://your-server:11434"
            
            // ═══════════════════════════════════════════════════════════
            
            // Validate API key
            if (apiKey.startsWith("YOUR_") || apiKey.isBlank()) {
                val providerName = when (provider) {
                    AIProvider.GEMINI -> "Google Gemini"
                    AIProvider.HUGGING_FACE -> "Hugging Face"
                    AIProvider.OPENAI -> "OpenAI"
                    AIProvider.OLLAMA -> "Ollama"
                    else -> provider.displayName
                }
                
                val getKeyUrl = when (provider) {
                    AIProvider.GEMINI -> "https://makersuite.google.com/app/apikey"
                    AIProvider.HUGGING_FACE -> "https://huggingface.co/settings/tokens"
                    AIProvider.OPENAI -> "https://platform.openai.com/api-keys"
                    else -> ""
                }
                
                statusText.text = "⚠️ Please add your $providerName API key in MainActivity.kt"
                statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                
                resultText.text = buildString {
                    append("📝 How to get started:\n\n")
                    append("1. Get a FREE API key from:\n")
                    append("   $getKeyUrl\n\n")
                    append("2. Copy the key\n\n")
                    append("3. Open MainActivity.kt\n\n")
                    append("4. Replace '$apiKey' with your actual key\n\n")
                    
                    if (provider.isFree) {
                        append("✅ ${provider.displayName} is completely FREE!")
                    } else {
                        append("💰 ${provider.displayName} has usage costs")
                    }
                }
                resultText.setTextColor(getColor(android.R.color.darker_gray))
                
                Toast.makeText(
                    this,
                    "Add your $providerName API key to continue",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            
            // Initialize with user session data
            val userSession = mapOf(
                "name" to "El Cipher",
                "age" to "22",
                "lastActivity" to "Created 3 romantic frames",
                "photosEdited" to "15"
            )
            
            NotificationAI.init(
                provider = provider,
                apiKey = apiKey,
                userSession = userSession
            )
            
            val freeLabel = if (provider.isFree) " (FREE ✅)" else ""
            statusText.text = "✓ ${provider.displayName}$freeLabel initialized successfully"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
            
            resultText.text = buildString {
                append("🎉 Ready to generate notifications!\n\n")
                append("Provider: ${provider.displayName}\n")
                if (provider.isFree) {
                    append("Cost: FREE! 🎁\n")
                } else {
                    append("Cost: ~$0.001 per notification\n")
                }
                append("\nUser: El Cipher, age 22\n")
                append("App: Instagram\n\n")
                append("Click a button below to test!")
            }
            // Use theme-aware text color for dark mode support
            resultText.setTextColor(getThemeTextColor())
            
            // Enable buttons
            btnGenerate.isEnabled = true
            btnGenerateVariants.isEnabled = true
            
        } catch (e: Exception) {
            statusText.text = "✗ Initialization failed: ${e.message}"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
            
            resultText.text = "Error: ${e.message}\n\nPlease check your API key and try again."
            resultText.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }
    
    /**
     * Example 1: Generate a single personalized notification
     */
    private fun generateSingleNotification() {
        showLoading(true)
        
        NotificationAI.generateNotification(
            appPackageName = "com.instagram.android",
            tone = NotificationTone.PLAYFUL,
            crashInfo = null,
            locale = "en"
        ) { result ->
            runOnUiThread {
                showLoading(false)
                
                when (result) {
                    is NotificationResult.Success -> {
                        val provider = NotificationAI.getCurrentProvider()
                        val textContent = buildString {
                            append("🎉 Generated Notification:\n\n")
                            append("\"${result.notification}\"\n\n")
                            append("━━━━━━━━━━━━━━━━━━━━\n")
                            append("Provider: ${provider.displayName}\n")
                            if (result.tokensUsed != null) {
                                append("Tokens used: ${result.tokensUsed}\n")
                            }
                            append("Generated at: ${formatTime(result.generatedAt)}\n")
                            if (provider.isFree) {
                                append("Cost: FREE! 🎁")
                            }
                        }
                        resultText.text = textContent
                        // Use theme-aware text color for dark mode support
                        // Set color after text to ensure it's applied
                        resultText.setTextColor(getThemeTextColor())
                        resultText.visibility = View.VISIBLE
                        
                        Toast.makeText(
                            this,
                            "Notification generated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    
                    is NotificationResult.Error -> {
                        resultText.text = buildString {
                            append("❌ Error:\n\n")
                            append("${result.message}\n\n")
                            if (result.message?.contains("loading") == true) {
                                append("💡 Tip: First request to Hugging Face can take 20-30 seconds. Please try again!")
                            }
                        }
                        resultText.setTextColor(getColor(android.R.color.holo_red_dark))
                        
                        Toast.makeText(
                            this,
                            "Failed to generate notification",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    /**
     * Example 2: Generate multiple notification variants for A/B testing
     */
    private fun generateNotificationVariants() {
        showLoading(true)
        
        NotificationAI.generateVariants(
            appPackageName = "com.instagram.android",
            count = 3,
            tone = NotificationTone.HUMOROUS
        ) { results ->
            runOnUiThread {
                showLoading(false)
                
                val successResults = results.filterIsInstance<NotificationResult.Success>()
                val errorResults = results.filterIsInstance<NotificationResult.Error>()
                
                if (successResults.isNotEmpty()) {
                    val provider = NotificationAI.getCurrentProvider()
                    val textContent = buildString {
                        append("🎯 A/B Test Variants:\n\n")
                        append("Provider: ${provider.displayName}\n")
                        if (provider.isFree) append("Cost: FREE! 🎁\n")
                        append("\n")
                        
                        successResults.forEachIndexed { index, result ->
                            append("Variant ${index + 1}:\n")
                            append("\"${result.notification}\"\n\n")
                        }
                        
                        if (errorResults.isNotEmpty()) {
                            append("━━━━━━━━━━━━━━━━━━━━\n")
                            append("⚠️ ${errorResults.size} variant(s) failed to generate")
                        } else {
                            append("━━━━━━━━━━━━━━━━━━━━\n")
                            append("💡 Test each variant to see which performs best!")
                        }
                    }
                    resultText.text = textContent
                    // Use theme-aware text color for dark mode support
                    resultText.setTextColor(getThemeTextColor())
                    resultText.visibility = View.VISIBLE
                    
                    Toast.makeText(
                        this,
                        "Generated ${successResults.size} variants!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    resultText.text = buildString {
                        append("❌ All variants failed:\n\n")
                        errorResults.forEachIndexed { index, error ->
                            append("${index + 1}. ${error.message}\n\n")
                        }
                        if (errorResults.firstOrNull()?.message?.contains("loading") == true) {
                            append("💡 Tip: First request to Hugging Face takes 20-30 seconds. Try again!")
                        }
                    }
                    resultText.setTextColor(getColor(android.R.color.holo_red_dark))
                }
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnGenerate.isEnabled = !show
        btnGenerateVariants.isEnabled = !show
        
        if (show) {
            resultText.text = "Generating notification..."
            resultText.setTextColor(getColor(android.R.color.darker_gray))
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        val dateFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(timestamp))
    }
    
    /**
     * Get the primary text color from the current theme
     * This ensures text is readable in both light and dark modes
     */
    private fun getThemeTextColor(): Int {
        // First, try to get the color from the theme attribute
        return try {
            val typedValue = TypedValue()
            val resolved = theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            if (resolved && typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && 
                typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT && typedValue.data != 0) {
                typedValue.data
            } else {
                // Fallback: use resources with theme
                resources.getColor(android.R.attr.textColorPrimary, theme)
            }
        } catch (e: Exception) {
            // Final fallback: detect dark mode and use appropriate color
            getColorForTheme()
        }
    }
    
    /**
     * Get color based on current theme (light/dark)
     * This is a reliable fallback that always works
     */
    private fun getColorForTheme(): Int {
        val isDark = (resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        return if (isDark) {
            // Dark mode: use white or light gray
            resources.getColor(android.R.color.white, theme)
        } else {
            // Light mode: use black or dark gray
            resources.getColor(android.R.color.black, theme)
        }
    }
    
    /**
     * Copy the result text to clipboard
     */
    private fun copyToClipboard() {
        val textToCopy = resultText.text.toString()
        if (textToCopy.isBlank() || textToCopy == "Click a button above to generate notifications") {
            Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
            return
        }
        
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Notification Result", textToCopy)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Example: Schedule daily background notifications
     * 
     * This demonstrates how to set up automatic notifications that:
     * - Run in the background
     * - Work when app is closed
     * - Survive phone reboots
     * - Show at a specific time each day
     */
    private fun scheduleBackgroundNotifications() {
        try {
            // Schedule daily notification at 10 AM
            NotificationAI.scheduleDaily(
                context = applicationContext,
                hour = 10,
                minute = 0,
                userId = "user123",
                tone = NotificationTone.FRIENDLY
            )
            
            Toast.makeText(
                this,
                "Background notifications scheduled for 10 AM daily!",
                Toast.LENGTH_LONG
            ).show()
            
            // Or schedule immediate notification for testing
            // NotificationAI.scheduleImmediate(
            //     context = applicationContext,
            //     tone = NotificationTone.PLAYFUL
            // )
            
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Failed to schedule: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

package com.example.team_gamma.data


import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_gamma.screens.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@HiltViewModel
class AiAssistantViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val faqDao = AppDatabase.getDatabase(application).appDao()

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-3.8-flash")
    }

    private fun isTransientError(e: Throwable): Boolean {
        val message = e.message?.lowercase() ?: ""
        val className = e.javaClass.name.lowercase()

        // Check for permanent errors that should NOT be retried
        if (message.contains("401") || message.contains("403") || message.contains("unauthorized") ||
            message.contains("forbidden") || message.contains("app check") ||
            message.contains("api_key_invalid") || message.contains("400") || message.contains("invalid argument") ||
            message.contains("billing") || message.contains("quota exceeded") || message.contains("prepayment")) {
            return false
        }

        // Check for retryable transient errors (503, high demand, overloaded, unavailable, timeout, IOException)
        return message.contains("503") || message.contains("high demand") ||
                message.contains("overloaded") || message.contains("unavailable") ||
                message.contains("timeout") || message.contains("rate limit") ||
                e is IOException ||
                className.contains("ioexception") || className.contains("sockettimeoutexception")
    }

    fun sendMessage(userInput: String) {
        _messages.add(ChatMessage(userInput, isFromUser = true))

        viewModelScope.launch {
            val app = getApplication<Application>()

            // 1. Check network connectivity FIRST
            if (!isNetworkAvailable(app)) {
                Log.d("AiAssistantViewModel", "[Offline FAQ Fallback] No network connection available.")
                queryOfflineFallback(userInput)
                return@launch
            }

            // 2. We have network. Try online Gemini API with bounded exponential backoff (max 3 attempts)
            var responseText: String? = null
            var lastException: Throwable? = null
            val maxAttempts = 3

            for (attempt in 1..maxAttempts) {
                try {
                    Log.d("AiAssistantViewModel", "[Online Gemini: gemini-3.8-flash] Attempt $attempt/$maxAttempts for prompt: $userInput")
                    val result = withContext(Dispatchers.IO) {
                        generativeModel.generateContent(userInput).text
                    }
                    if (!result.isNullOrBlank()) {
                        responseText = result
                        Log.d("AiAssistantViewModel", "[Online Gemini: gemini-3.8-flash] Successful response on attempt $attempt.")
                        break
                    }
                } catch (e: Throwable) {
                    lastException = e
                    Log.w("AiAssistantViewModel", "[Online Gemini] Attempt $attempt failed: ${e.message}", e)

                    // Check if error is permanent (non-transient). If so, break immediately without retries.
                    if (!isTransientError(e)) {
                        Log.w("AiAssistantViewModel", "[Online Gemini] Encountered permanent error. Aborting retries.")
                        break
                    }

                    // If not the last attempt, wait with exponential backoff (attempt 2: 1s, attempt 3: 2s)
                    if (attempt < maxAttempts) {
                        val delayMs = (1000L * (1 shl (attempt - 1))).coerceAtMost(4000L)
                        Log.d("AiAssistantViewModel", "[Online Gemini] Waiting ${delayMs}ms before retry attempt ${attempt + 1}...")
                        delay(delayMs)
                    }
                }
            }

            // 3. Handle successful online response
            if (!responseText.isNullOrBlank()) {
                _messages.add(ChatMessage(responseText, isFromUser = false))
                return@launch
            }

            // 4. All online attempts failed or aborted. Fall back to local Room FAQ database.
            Log.d("AiAssistantViewModel", "[Offline FAQ Fallback] Online Gemini API attempts failed. Falling back to local FAQ DB.")
            val fallbackHandled = queryOfflineFallback(userInput)
            if (!fallbackHandled) {
                // No local FAQ fallback found; show user-friendly service unavailable message categorized by error type
                val errorMessage = lastException?.message ?: ""
                val causeMessage = lastException?.cause?.message ?: ""
                val className = lastException?.javaClass?.name ?: ""

                when {
                    className.contains("SerializationException", ignoreCase = true) || errorMessage.contains("SerializationException", ignoreCase = true) || errorMessage.contains("MissingFieldException", ignoreCase = true) || causeMessage.contains("MissingFieldException", ignoreCase = true) -> {
                        _messages.add(ChatMessage("Temporary Service Error: Failed to parse error response from AI service (Model might be unavailable or invalid response received).", isFromUser = false))
                    }
                    errorMessage.contains("quota", ignoreCase = true) || errorMessage.contains("429") -> {
                        _messages.add(ChatMessage("Service Limit Reached: The AI Assistant has exceeded its request quota. Please try again later.", isFromUser = false))
                    }
                    errorMessage.contains("timeout", ignoreCase = true) || errorMessage.contains("SocketTimeoutException", ignoreCase = true) -> {
                        _messages.add(ChatMessage("Temporary Service Error: The request timed out due to high demand. Please try again.", isFromUser = false))
                    }
                    else -> {
                        _messages.add(ChatMessage("Temporary Service Error: The AI service is currently experiencing high demand and is temporarily unavailable. Please try again later.", isFromUser = false))
                    }
                }
            }
        }
    }

    private suspend fun queryOfflineFallback(userInput: String): Boolean {
        return withContext(Dispatchers.IO) {
            val offlineAnswer = faqDao.findAnswer(userInput)
            if (offlineAnswer != null) {
                withContext(Dispatchers.Main) {
                    _messages.add(ChatMessage("[Offline Fallback] ${offlineAnswer.answer}", isFromUser = false))
                }
                true
            } else {
                false
            }
        }
    }
}

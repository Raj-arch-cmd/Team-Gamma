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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AiAssistantViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val faqDao = AppDatabase.getDatabase(application).appDao()

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-3.8-flash")
    }

    fun sendMessage(userInput: String) {
        _messages.add(ChatMessage(userInput, isFromUser = true))

        viewModelScope.launch {
            val app = getApplication<Application>()

            // 1. Check network connectivity FIRST
            if (!isNetworkAvailable(app)) {
                // No internet -> Fallback to Offline FAQ
                Log.d("AiAssistantViewModel", "[Offline FAQ Fallback] No network connection available.")
                val offlineAnswer = withContext(Dispatchers.IO) {
                    faqDao.findAnswer(userInput)
                }
                if (offlineAnswer != null) {
                    _messages.add(ChatMessage(offlineAnswer.answer, isFromUser = false))
                } else {
                    _messages.add(ChatMessage("Offline Mode: You are not connected to the internet. For offline help, try asking about floods, burns, or an emergency kit.", isFromUser = false))
                }
                return@launch
            }

            // 2. We have network. Try the online Firebase AI Logic Gemini API
            try {
                Log.d("AiAssistantViewModel", "[Online Gemini: gemini-3.8-flash] Sending prompt: $userInput")
                val responseText = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(userInput).text
                }
                if (!responseText.isNullOrBlank()) {
                    Log.d("AiAssistantViewModel", "[Online Gemini: gemini-3.8-flash] Received successful response.")
                    _messages.add(ChatMessage(responseText, isFromUser = false))
                } else {
                    _messages.add(ChatMessage("Temporary Service Error: I'm having trouble retrieving a response.", isFromUser = false))
                }
            } catch (e: Exception) {
                Log.e("AiAssistantViewModel", "[Online Gemini] API call failed: ${e.message}", e)
                val errorMessage = e.message ?: ""
                val causeMessage = e.cause?.message ?: ""
                
                // Categorize errors
                val className = e.javaClass.name
                when {
                    className.contains("SerializationException", ignoreCase = true) || errorMessage.contains("SerializationException", ignoreCase = true) || errorMessage.contains("MissingFieldException", ignoreCase = true) || causeMessage.contains("MissingFieldException", ignoreCase = true) -> {
                        _messages.add(ChatMessage("Temporary Service Error: Failed to parse error response from AI service (Model might be unavailable or invalid response received).", isFromUser = false))
                    }
                    errorMessage.contains("quota", ignoreCase = true) || errorMessage.contains("429") -> {
                        _messages.add(ChatMessage("Service Limit Reached: The AI Assistant has exceeded its request quota. Please try again later.", isFromUser = false))
                    }
                    errorMessage.contains("timeout", ignoreCase = true) -> {
                        _messages.add(ChatMessage("Temporary Service Error: The request timed out. Please try again.", isFromUser = false))
                    }
                    else -> {
                        _messages.add(ChatMessage("Temporary Service Error: The AI service is currently unavailable. (${e.localizedMessage})", isFromUser = false))
                    }
                }
            }
        }
    }
}

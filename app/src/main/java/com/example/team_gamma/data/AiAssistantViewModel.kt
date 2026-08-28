package com.example.team_gamma.data


import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_gamma.BuildConfig
import com.example.team_gamma.screens.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class AiAssistantViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val faqDao = AppDatabase.getDatabase(application).appDao()

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    private val geminiApiKey = BuildConfig.GEMINI_API_KEY

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-pro",
            apiKey = geminiApiKey
        )
    }

    fun sendMessage(userInput: String) {
        _messages.add(ChatMessage(userInput, isFromUser = true))

        viewModelScope.launch(Dispatchers.IO) {
            // Check if the API key is empty or not set.
            if (geminiApiKey.isBlank() || geminiApiKey == "null") {
                _messages.add(ChatMessage("API Key not found. Please add your Gemini API key to your local.properties file.", isFromUser = false))
                return@launch
            }

            try {
                // First, try the online Gemini API
                val response = generativeModel.generateContent(userInput)
                response.text?.let {
                    _messages.add(ChatMessage(it, isFromUser = false))
                }
            } catch (e: Exception) {
                Log.e("AiAssistantViewModel", "API call failed: ${e.message}")
                // If online fails, check the offline database
                val offlineAnswer = faqDao.findAnswer(userInput)
                if (offlineAnswer != null) {
                    _messages.add(ChatMessage(offlineAnswer.answer, isFromUser = false))
                } else {
                    _messages.add(ChatMessage("I'm having trouble connecting. Please check your internet or try asking about floods, burns, or an emergency kit for offline help.", isFromUser = false))
                }
            }
        }
    }
}


package com.example.team_gamma.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_gamma.FlaskApi.PredictionResponse // ✅ CORRECT IMPORT
import com.example.team_gamma.FlaskApi.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// This sealed interface now correctly holds the new, richer PredictionResponse
sealed interface PredictionUiState {
    object Loading : PredictionUiState
    data class Success(val prediction: PredictionResponse) : PredictionUiState
    data class Error(val message: String) : PredictionUiState
}

class FloodPredictionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PredictionUiState>(PredictionUiState.Loading)
    val uiState: StateFlow<PredictionUiState> = _uiState

    fun fetchPrediction() {
        viewModelScope.launch {
            _uiState.value = PredictionUiState.Loading
            try {
                val response = RetrofitClient.instance.getPrediction()
                _uiState.value = PredictionUiState.Success(response)
            } catch (e: Exception) {
                val errorMessage = when {
                    e is java.net.SocketTimeoutException -> "Connection timeout - server not responding"
                    e is java.net.ConnectException -> "Cannot connect to server - check if Flask is running"
                    e is java.net.UnknownHostException -> "Server address not found"
                    else -> "Network error: ${e.message}"
                }
                _uiState.value = PredictionUiState.Error(errorMessage)
            }
        }
    }
}
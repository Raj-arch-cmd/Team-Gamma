package com.example.team_gamma.data

import com.example.team_gamma.FlaskApi.PredictionResponse
import com.example.team_gamma.FlaskApi.RetrofitClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PredictionUiState {
    object Loading : PredictionUiState
    data class Success(val prediction: PredictionResponse) : PredictionUiState
    data class Error(val message: String) : PredictionUiState
}

class FloodPredictionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PredictionUiState>(PredictionUiState.Loading)
    val uiState: StateFlow<PredictionUiState> = _uiState

    // THIS IS THE FIX: The function no longer needs parameters.
    fun fetchPrediction() {
        viewModelScope.launch {
            _uiState.value = PredictionUiState.Loading
            try {
                // The call now has no arguments.
                val response = RetrofitClient.instance.getPrediction()
                _uiState.value = PredictionUiState.Success(response)
            } catch (e: Exception) {
                _uiState.value = PredictionUiState.Error("Failed to connect to server: ${e.message}")
            }
        }
    }
}
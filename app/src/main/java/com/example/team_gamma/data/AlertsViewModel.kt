package com.example.team_gamma.data


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// This sealed interface represents the different states our UI can be in.
sealed interface AlertsUiState {
    object Loading : AlertsUiState
    data class Success(val alerts: List<HistoricalAlert>) : AlertsUiState
    data class Error(val message: String) : AlertsUiState
}

class AlertsViewModel : ViewModel() {
    private val alertsRepository = AlertsRepository()

    private val _uiState = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        // Automatically fetch alerts when the ViewModel is created.
        fetchAlerts(18.5204, 73.8567) // Using a placeholder location for now
    }

    fun fetchAlerts(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = AlertsUiState.Loading
            alertsRepository.getHistoricalAlerts(latitude, longitude)
                .onSuccess { alerts ->
                    _uiState.value = AlertsUiState.Success(alerts)
                }
                .onFailure { error ->
                    _uiState.value = AlertsUiState.Error(error.message ?: "An unknown error occurred")
                }
        }
    }
}

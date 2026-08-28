package com.example.team_gamma.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MLPredictionViewModel(
    private val mlPredictionRepository: MLPredictionRepository = MLPredictionRepository()
) : ViewModel() {

    private val _predictionState = MutableStateFlow<PredictionState>(PredictionState.Idle)
    val predictionState: StateFlow<PredictionState> = _predictionState

    fun predictDisaster(sensorData: List<Double>) {
        viewModelScope.launch {
            _predictionState.value = PredictionState.Loading
            val result = mlPredictionRepository.predictDisaster(sensorData)
            _predictionState.value = when {
                result.isSuccess -> PredictionState.Success(result.getOrThrow())
                else -> PredictionState.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun resetState() {
        _predictionState.value = PredictionState.Idle
    }

    sealed class PredictionState {
        object Idle : PredictionState()
        object Loading : PredictionState()
        data class Success(val result: MLPredictionResult) : PredictionState()
        data class Error(val message: String) : PredictionState()
    }
}
package com.example.team_gamma.data

import androidx.lifecycle.ViewModel
import com.example.team_gamma.data.EvacuationRoute
import com.example.team_gamma.data.PredictionResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface PredictionUiState {
    object Loading : PredictionUiState
    object Neutral : PredictionUiState
    data class Success(val prediction: PredictionResponse) : PredictionUiState
    data class Error(val message: String) : PredictionUiState
}

/**
 * Formerly FloodPredictionViewModel.
 * Now provides neutral status and static evacuation data to ensure app stability
 * without relying on external ML/Flask servers.
 */
@HiltViewModel
class FloodPredictionViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<PredictionUiState>(PredictionUiState.Neutral)
    val uiState: StateFlow<PredictionUiState> = _uiState.asStateFlow()

    // Static data for shelters/routes to be used when live data is unavailable
    private val staticEvacuationData = PredictionResponse(
        status = "DATA_UNAVAILABLE",
        riskPercentage = null,
        evacuationMapUrl = null,
        riskAssessment = null,
        evacuationRoutes = listOf(
            EvacuationRoute("Community Center Alpha", 0.45, "HIGH"),
            EvacuationRoute("St. Jude School Annex", 0.82, "HIGH"),
            EvacuationRoute("Central Park Heights", 1.20, "MEDIUM")
        )
    )

    fun fetchPrediction() {
        // No longer making network calls. Directly providing neutral/static state.
        _uiState.value = PredictionUiState.Success(staticEvacuationData)
    }
}

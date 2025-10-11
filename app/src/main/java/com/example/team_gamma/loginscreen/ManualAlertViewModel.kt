package com.example.team_gamma.loginscreen

// This file should be located at: app/src/main/java/com/example/team_gamma/data/ManualAlertViewModel.kt


import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * This sealed class defines all the possible states your manual alert can be in.
 * Each state holds the title, message, and color for the UI.
 */
sealed class ManualAlertState(val title: String, val message: String, val color: Color) {
    object NoRisk : ManualAlertState("All Clear", "No immediate risk detected.", Color(0xFF00C853)) // Green
    object LowRisk : ManualAlertState("Low Risk", "Monitor conditions. Minor disruptions possible.", Color(0xFFFFD600)) // Yellow
    object MediumRisk : ManualAlertState("Medium Risk", "Rainfall started. Be aware of potential flooding.", Color(0xFFFFA000)) // Orange
    object HighRisk : ManualAlertState("High Risk", "Flooding likely. Prepare for possible evacuation.", Color.Red)
    object ExtremeRisk : ManualAlertState("Extreme Risk", "Severe flooding imminent! Evacuate NOW!", Color(0xFFB71C1C)) // Dark Red
}

/**
 * This is your "remote control". It holds the current alert state
 * and provides functions (the "buttons") to change it.
 */
class ManualAlertViewModel : ViewModel() {

    // The private internal state that holds the current alert status.
    private val _uiState = MutableStateFlow<ManualAlertState>(ManualAlertState.NoRisk)

    // The public state that your screen will watch for changes.
    val uiState: StateFlow<ManualAlertState> = _uiState

    // --- These are the "buttons" on your remote control ---

    fun setNoRisk() { _uiState.value = ManualAlertState.NoRisk }
    fun setLowRisk() { _uiState.value = ManualAlertState.LowRisk }
    fun setMediumRisk() { _uiState.value = ManualAlertState.MediumRisk }
    fun setHighRisk() { _uiState.value = ManualAlertState.HighRisk }
    fun setExtremeRisk() { _uiState.value = ManualAlertState.ExtremeRisk }

    /**
     * A helper function to easily cycle through the states for a demo.
     * No Risk -> Medium Risk -> High Risk -> No Risk
     */
    fun cycleState() {
        _uiState.value = when (_uiState.value) {
            is ManualAlertState.NoRisk -> ManualAlertState.MediumRisk
            is ManualAlertState.MediumRisk -> ManualAlertState.HighRisk
            else -> ManualAlertState.NoRisk
        }
    }
}
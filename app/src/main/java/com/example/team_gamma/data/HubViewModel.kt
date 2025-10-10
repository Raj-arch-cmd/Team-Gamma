package com.example.team_gamma.data


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HubViewModel : ViewModel() {

    // This holds the current alert. If it's null, there is no emergency.
    private val _activeAlert = MutableStateFlow<EmergencyAlert?>(null)
    val activeAlert: StateFlow<EmergencyAlert?> = _activeAlert.asStateFlow()

    // This function simulates receiving an emergency alert.
    // In a real app, this would be triggered by a push notification from a server.
    fun triggerTestAlert() {
        _activeAlert.value = EmergencyAlert(
            title = "SEVERE FLOOD WARNING",
            description = "Heavy rainfall has caused the Mula river to overflow. Low-lying areas are at high risk.",
            locationName = "Pimpri-Chinchwad",
            severity = AlertSeverity.HIGH
            // We can add dangerZone and safeRoute coordinates here later
        )
    }

    // This function simulates the "all clear" signal.
    fun clearAlert() {
        _activeAlert.value = null
    }
}

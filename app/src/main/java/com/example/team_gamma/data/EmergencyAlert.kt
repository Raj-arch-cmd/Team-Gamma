package com.example.team_gamma.data


import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng

// Enum to define the severity of an alert
enum class AlertSeverity(val color: Color) {
    HIGH(Color(0xFFB00020)), // Dark Red
    MEDIUM(Color(0xFFFF8F00)), // Amber
    LOW(Color(0xFFFFEB3B))  // Yellow
}

// This data class holds all the information for an active emergency alert.
data class EmergencyAlert(
    val title: String,
    val description: String,
    val locationName: String,
    val severity: AlertSeverity,
    // These will be used later to draw on the map
    val dangerZone: List<LatLng>? = null,
    val safeRoute: List<LatLng>? = null
)

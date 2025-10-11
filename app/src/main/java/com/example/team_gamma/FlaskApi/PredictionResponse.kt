package com.example.team_gamma.FlaskApi

// File: PredictionResponse.kt
data class PredictionResponse(
    val status: String, // e.g., "all_clear", "rainfall_started", "high_risk"
    val message: String,
    val risk_percentage: Double?, // Nullable because it might not always be there
    val population_at_risk: Int?, // Nullable
    val map_url: String?,         // Nullable
    val evacuation_map_url: String? // Nullable
)
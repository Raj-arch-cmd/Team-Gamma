package com.example.team_gamma.data


// No special imports needed for basic data classes
data class MLPredictionRequest(
    val input_data: List<Double>,
    val features: List<String>? = null,
    val timestamp: String? = null
)

data class MLPredictionResponse(
    val prediction: String,
    val confidence: Double,
    val disaster_type: String?,
    val severity: String?,
    val recommended_actions: List<String>?,
    val risk_level: String?
)

data class MLPredictionResult(
    val isDisaster: Boolean,
    val type: String,
    val confidence: Double,
    val severity: String,
    val riskLevel: String,
    val recommendedActions: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)
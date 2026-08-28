package com.example.team_gamma.data

import com.google.gson.annotations.SerializedName

// Main response class
data class PredictionResponse(
    @SerializedName("state")
    val status: String,

    @SerializedName("risk_score")
    val riskPercentage: Double?,

    @SerializedName("evacuation_map")
    val evacuationMapUrl: String?,

    // Nested object for risk assessment
    @SerializedName("risk_assessment")
    val riskAssessment: RiskAssessment?,

    // List of nested objects for evacuation routes
    @SerializedName("evacuation_routes")
    val evacuationRoutes: List<EvacuationRoute>?
)

// Class for the "risk_assessment" JSON object
data class RiskAssessment(
    @SerializedName("elevation_risk")
    val elevationRisk: Double,

    @SerializedName("population_risk")
    val populationRisk: Double,

    @SerializedName("total_risk")
    val totalRisk: Double
)

// Class for each item in the "evacuation_routes" JSON array
data class EvacuationRoute(
    @SerializedName("shelter")
    val shelter: String,

    @SerializedName("distance_km")
    val distanceKm: Double,

    @SerializedName("priority")
    val priority: String
)
package com.example.team_gamma.FlaskApi

import com.google.gson.annotations.SerializedName

// This data class now perfectly matches the JSON from your friend's server.
data class PredictionResponse(

    @SerializedName("state")
    val status: String, // "HIGH_RISK", "RAINFALL_STARTED", "CLEAR"

    @SerializedName("risk_score")
    val riskPercentage: Double?,

    @SerializedName("evacuation_map")
    val evacuationMapUrl: String?,

    // We will add a custom message in the app, since the server doesn't provide one.
    val shelters: List<Any>? // We can ignore the shelter details for now
)
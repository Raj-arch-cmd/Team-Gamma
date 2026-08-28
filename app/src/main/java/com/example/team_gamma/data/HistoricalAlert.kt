package com.example.team_gamma.data

import com.google.gson.annotations.SerializedName

data class HistoricalAlert(
    @SerializedName("disaster_type") val disasterType: String,
    @SerializedName("date") val date: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("details") val details: String
)

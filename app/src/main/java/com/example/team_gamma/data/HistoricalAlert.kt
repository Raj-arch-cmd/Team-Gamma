package com.example.team_gamma.data


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HistoricalAlert(
    @Json(name = "disaster_type") val disasterType: String,
    @Json(name = "date") val date: String,
    @Json(name = "severity") val severity: String,
    @Json(name = "details") val details: String
)
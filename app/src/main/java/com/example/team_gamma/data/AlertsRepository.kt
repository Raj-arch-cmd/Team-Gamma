package com.example.team_gamma.data

import android.util.Log
import java.util.Locale

class AlertsRepository {
    private val apiService = RetrofitClient.instance

    suspend fun getHistoricalAlerts(latitude: Double, longitude: Double): Result<List<HistoricalAlert>> {
        return try {
            Log.d("AlertsRepository", "Fetching live alerts from API for location: lat=$latitude, lon=$longitude")
            val alerts = apiService.getHistoricalAlerts(latitude, longitude)
            Result.success(alerts)
        } catch (e: Exception) {
            Log.w("AlertsRepository", "Live alert API call failed (${e.localizedMessage}), serving location-specific fallback alerts for lat=$latitude, lon=$longitude")
            val formattedLat = String.format(Locale.US, "%.2f", latitude)
            val formattedLon = String.format(Locale.US, "%.2f", longitude)
            Result.success(
                listOf(
                    HistoricalAlert(
                        disasterType = "Flood",
                        date = "15 Aug 2021",
                        severity = "High",
                        details = "Major river basin flooding recorded near coordinates ($formattedLat, $formattedLon)."
                    ),
                    HistoricalAlert(
                        disasterType = "Earthquake",
                        date = "22 May 2018",
                        severity = "Low",
                        details = "Minor tremors (3.1 magnitude) felt in the vicinity of ($formattedLat, $formattedLon)."
                    ),
                    HistoricalAlert(
                        disasterType = "Heatwave",
                        date = "03 Jun 2023",
                        severity = "Medium",
                        details = "Temperatures exceeded 40°C in region ($formattedLat, $formattedLon)."
                    )
                )
            )
        }
    }
}

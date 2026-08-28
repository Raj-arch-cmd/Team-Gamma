package com.example.team_gamma.data

import com.example.team_gamma.data.HistoricalAlert
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    // --- Historical Alerts Endpoint ---
    @GET("alerts")
    suspend fun getHistoricalAlerts(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): List<HistoricalAlert>
}
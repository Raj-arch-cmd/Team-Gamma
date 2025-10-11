package com.example.team_gamma.FlaskApi

import com.example.team_gamma.data.HistoricalAlert
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    // THIS IS THE FIX: Changed from @POST("predict") to @GET("run_quick")
    // and removed the @Body parameter.
    @GET("run_quick")
    suspend fun getPrediction(): PredictionResponse // It no longer sends location data

    // --- Historical Alerts Endpoint (Stays the same) ---
    @GET("alerts")
    suspend fun getHistoricalAlerts(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): List<HistoricalAlert>
}
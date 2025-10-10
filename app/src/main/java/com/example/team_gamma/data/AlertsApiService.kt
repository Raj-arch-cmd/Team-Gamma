package com.example.team_gamma.data



import retrofit2.http.GET
import retrofit2.http.Query

interface AlertsApiService {
    // This defines a function to get alerts from the server.
    // The final URL will be something like: your_base_url/alerts?lat=18.52&lon=73.85
    @GET("alerts")
    suspend fun getHistoricalAlerts(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): List<HistoricalAlert>
}

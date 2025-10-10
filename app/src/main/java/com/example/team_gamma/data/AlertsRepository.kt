package com.example.team_gamma.data


import kotlinx.coroutines.delay

class AlertsRepository {
    private val apiService = RetrofitClient.instance

    suspend fun getHistoricalAlerts(latitude: Double, longitude: Double): Result<List<HistoricalAlert>> {
        // FOR NOW, WE WILL RETURN FAKE DATA.
        // When your friend's backend is ready, you will delete this fake data
        // and uncomment the real network call below.

        // --- FAKE DATA (DELETE LATER) ---
        delay(1500) // Simulate a network delay
        return Result.success(
            listOf(
                HistoricalAlert("Flood", "15 Aug 2021", "High", "Major flooding in Mula-Mutha river basin."),
                HistoricalAlert("Earthquake", "22 May 2018", "Low", "Minor tremors (3.1 magnitude) felt across the city."),
                HistoricalAlert("Heatwave", "03 Jun 2023", "Medium", "Temperatures reached 42°C for three consecutive days.")
            )
        )
        // --- END OF FAKE DATA ---


        /*
        // --- REAL NETWORK CALL (UNCOMMENT LATER) ---
        return try {
            val alerts = apiService.getHistoricalAlerts(latitude, longitude)
            Result.success(alerts)
        } catch (e: Exception) {
            Result.failure(e)
        }
        // --- END OF REAL NETWORK CALL ---
        */
    }
}

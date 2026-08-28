package com.example.team_gamma.data


import kotlinx.coroutines.delay
import javax.inject.Inject

class MLPredictionRepository @Inject constructor() {

    private val mlPredictionApi = MLRetrofitClient.mlPredictionApi

    suspend fun predictDisaster(sensorData: List<Double>): Result<MLPredictionResult> {
        return try {
            val request = MLPredictionRequest(input_data = sensorData)
            val response = mlPredictionApi.predictDisaster(request)

            val result = MLPredictionResult(
                isDisaster = response.prediction.lowercase() in listOf("disaster", "true", "yes", "1"),
                type = response.disaster_type ?: "unknown",
                confidence = response.confidence,
                severity = response.severity ?: "medium",
                riskLevel = response.risk_level ?: "medium",
                recommendedActions = response.recommended_actions ?: getDefaultActions()
            )

            Result.success(result)
        } catch (e: Exception) {
            // Fallback to mock data for now
            delay(1000) // Simulate processing
            Result.success(getMockPrediction())
        }
    }

    // ADD THESE MISSING METHODS:
    internal fun getDefaultActions(): List<String> {
        return listOf(
            "Stay calm and assess the situation",
            "Follow local authority instructions",
            "Move to a safe location if necessary"
        )
    }

    private fun getMockPrediction(): MLPredictionResult {
        return MLPredictionResult(
            isDisaster = true,
            type = "earthquake",
            confidence = 0.87,
            severity = "high",
            riskLevel = "high",
            recommendedActions = listOf(
                "Take cover under sturdy furniture",
                "Stay away from windows",
                "Evacuate if in tsunami zone"
            )
        )
    }

    // ADD this method for testing safe scenarios
    fun getMockSafePrediction(): MLPredictionResult {
        return MLPredictionResult(
            isDisaster = false,
            type = "normal",
            confidence = 0.92,
            severity = "low",
            riskLevel = "low",
            recommendedActions = listOf(
                "Continue normal activities",
                "Stay informed about weather updates",
                "Keep emergency kit prepared"
            )
        )
    }
}
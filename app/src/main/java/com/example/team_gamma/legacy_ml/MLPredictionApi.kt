package com.example.team_gamma.data


import retrofit2.http.Body
import retrofit2.http.POST

interface MLPredictionApi {
    @POST("predict/")
    suspend fun predictDisaster(@Body request: MLPredictionRequest): MLPredictionResponse
}
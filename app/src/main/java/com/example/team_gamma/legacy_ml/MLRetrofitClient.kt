package com.example.team_gamma.data


import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object MLRetrofitClient {
    // Replace with your friend's server IP
    private const val BASE_URL = "http://10.0.2.2:8000/" // For emulator
    // private const val BASE_URL = "http://192.168.1.100:8000/" // For real device

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val mlPredictionApi: MLPredictionApi = retrofit.create(MLPredictionApi::class.java)
}
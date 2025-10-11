// File: app/src/main/java/com/example/team_gamma/FlaskApi/RetrofitClient.kt

package com.example.team_gamma.FlaskApi

// Make sure THIS import is correct
import com.example.team_gamma.FlaskApi.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.113.171.128:5000"
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        // ... (your existing okHttpClient code is perfect)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .build()

    // CHANGE #1: The instance is now of type ApiService
    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // CHANGE #2: Create the ApiService interface
        retrofit.create(ApiService::class.java)
    }
}
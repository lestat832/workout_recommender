package com.workoutapp.data.remote.strava

import com.workoutapp.util.StravaConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Strava API client singleton
 * Provides configured Retrofit instance for Strava API calls
 */
object StravaApiClient {

    /**
     * OkHttp client with logging and timeout configuration
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Retrofit instance configured for Strava API
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(StravaConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Strava API interface instance
     */
    val api: StravaApi by lazy {
        retrofit.create(StravaApi::class.java)
    }

    /**
     * Helper function to format authorization header
     */
    fun formatAuthHeader(accessToken: String): String {
        return "Bearer $accessToken"
    }

    /**
     * Check if API is configured with credentials
     */
    fun isConfigured(): Boolean {
        return StravaConfig.isConfigured()
    }
}

package com.workoutapp.data.remote

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service for fetching exercises from ExerciseDB (Free Exercise DB)
 */
class ExerciseDbService(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private const val TAG = "ExerciseDbService"
        private const val EXERCISES_URL =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json"
        private const val EXERCISES_ASSET = "exercises.json"
    }

    /**
     * Fetches all exercises from the API
     * Falls back to bundled JSON asset if network request fails
     */
    suspend fun fetchAllExercises(): Result<List<ExerciseDbJson>> = withContext(Dispatchers.IO) {
        try {
            // Try network first
            val exercises = fetchFromNetwork()
            if (exercises != null) {
                Log.d(TAG, "Successfully fetched ${exercises.size} exercises from network")
                return@withContext Result.success(exercises)
            }

            // Fallback to bundled asset
            Log.w(TAG, "Network fetch failed, falling back to bundled exercises")
            val assetExercises = loadFromAssets()
            if (assetExercises != null) {
                Log.d(TAG, "Successfully loaded ${assetExercises.size} exercises from assets")
                return@withContext Result.success(assetExercises)
            }

            Result.failure(IOException("Failed to load exercises from network and assets"))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching exercises", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches exercises from network
     */
    private fun fetchFromNetwork(): List<ExerciseDbJson>? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(EXERCISES_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<List<ExerciseDbJson>>(jsonString)
            } else {
                Log.e(TAG, "Network request failed with code: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network fetch exception", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Loads exercises from bundled assets
     * Use this as a fallback when network is unavailable
     */
    private fun loadFromAssets(): List<ExerciseDbJson>? {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.assets.open(EXERCISES_ASSET)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<List<ExerciseDbJson>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Asset load exception", e)
            null
        } finally {
            inputStream?.close()
        }
    }
}

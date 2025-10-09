package com.workoutapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity storing Strava OAuth authentication tokens
 * Only one row should exist (singleton pattern)
 */
@Entity(tableName = "strava_auth")
data class StravaAuthEntity(
    @PrimaryKey
    val id: Int = 1, // Singleton - always use ID 1

    /**
     * OAuth access token for Strava API calls
     */
    val accessToken: String,

    /**
     * Refresh token for obtaining new access tokens
     */
    val refreshToken: String,

    /**
     * Unix timestamp when access token expires
     */
    val expiresAt: Long,

    /**
     * Strava athlete ID
     */
    val athleteId: Long,

    /**
     * When the tokens were last refreshed
     */
    val lastRefreshedAt: Long = System.currentTimeMillis(),

    /**
     * OAuth scope granted (e.g., "activity:write")
     */
    val scope: String = "activity:write"
) {
    /**
     * Check if access token is expired or will expire within buffer time
     */
    fun isExpired(bufferSeconds: Long = 300): Boolean {
        val currentTime = System.currentTimeMillis() / 1000
        return currentTime >= (expiresAt - bufferSeconds)
    }

    /**
     * Check if currently authenticated
     */
    fun isAuthenticated(): Boolean {
        return !isExpired()
    }
}

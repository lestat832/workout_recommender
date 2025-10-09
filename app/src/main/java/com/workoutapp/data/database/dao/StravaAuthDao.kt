package com.workoutapp.data.database.dao

import androidx.room.*
import com.workoutapp.data.database.entities.StravaAuthEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StravaAuthDao {

    /**
     * Insert or update Strava auth tokens (singleton)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(auth: StravaAuthEntity)

    /**
     * Get current Strava auth (always ID = 1)
     */
    @Query("SELECT * FROM strava_auth WHERE id = 1 LIMIT 1")
    suspend fun getAuth(): StravaAuthEntity?

    /**
     * Get current Strava auth as Flow
     */
    @Query("SELECT * FROM strava_auth WHERE id = 1 LIMIT 1")
    fun getAuthFlow(): Flow<StravaAuthEntity?>

    /**
     * Update access token and expiry
     */
    @Query("""
        UPDATE strava_auth
        SET accessToken = :accessToken,
            expiresAt = :expiresAt,
            lastRefreshedAt = :timestamp
        WHERE id = 1
    """)
    suspend fun updateAccessToken(
        accessToken: String,
        expiresAt: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Update both access and refresh tokens
     */
    @Query("""
        UPDATE strava_auth
        SET accessToken = :accessToken,
            refreshToken = :refreshToken,
            expiresAt = :expiresAt,
            lastRefreshedAt = :timestamp
        WHERE id = 1
    """)
    suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
        expiresAt: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Delete Strava auth (logout)
     */
    @Query("DELETE FROM strava_auth WHERE id = 1")
    suspend fun deleteAuth()

    /**
     * Check if user is authenticated
     */
    @Query("SELECT COUNT(*) > 0 FROM strava_auth WHERE id = 1")
    suspend fun isAuthenticated(): Boolean

    /**
     * Check if user is authenticated (Flow)
     */
    @Query("SELECT COUNT(*) > 0 FROM strava_auth WHERE id = 1")
    fun isAuthenticatedFlow(): Flow<Boolean>

    /**
     * Get access token
     */
    @Query("SELECT accessToken FROM strava_auth WHERE id = 1 LIMIT 1")
    suspend fun getAccessToken(): String?

    /**
     * Get refresh token
     */
    @Query("SELECT refreshToken FROM strava_auth WHERE id = 1 LIMIT 1")
    suspend fun getRefreshToken(): String?

    /**
     * Get athlete ID
     */
    @Query("SELECT athleteId FROM strava_auth WHERE id = 1 LIMIT 1")
    suspend fun getAthleteId(): Long?
}

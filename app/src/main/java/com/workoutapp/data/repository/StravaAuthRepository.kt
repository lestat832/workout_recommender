package com.workoutapp.data.repository

import com.workoutapp.data.database.dao.StravaAuthDao
import com.workoutapp.data.database.entities.StravaAuthEntity
import com.workoutapp.data.remote.strava.StravaApiClient
import com.workoutapp.util.StravaConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Strava authentication and token management
 */
@Singleton
class StravaAuthRepository @Inject constructor(
    private val stravaAuthDao: StravaAuthDao
) {

    private val stravaApi = StravaApiClient

    /**
     * Observe authentication state
     */
    val authState: Flow<StravaAuthEntity?> = stravaAuthDao.getAuthFlow()

    /**
     * Get current authentication entity
     */
    suspend fun getAuth(): StravaAuthEntity? {
        return stravaAuthDao.getAuth()
    }

    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean {
        val auth = getAuth()
        return auth?.isAuthenticated() == true
    }

    /**
     * Exchange authorization code for access tokens
     */
    suspend fun exchangeCodeForTokens(code: String): Result<StravaAuthEntity> {
        return try {
            val response = stravaApi.api.exchangeCodeForTokens(
                clientId = StravaConfig.CLIENT_ID,
                clientSecret = StravaConfig.CLIENT_SECRET,
                code = code
            )

            if (response.isSuccessful) {
                val tokenResponse = response.body()!!
                val authEntity = StravaAuthEntity(
                    id = 1, // Singleton
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresAt = tokenResponse.expiresAt,
                    athleteId = tokenResponse.athlete.id,
                    lastRefreshedAt = System.currentTimeMillis(),
                    scope = "activity:write"
                )

                // Save to database
                stravaAuthDao.insert(authEntity)

                Result.success(authEntity)
            } else {
                Result.failure(Exception("Failed to exchange code: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh access token using refresh token
     */
    suspend fun refreshAccessToken(): Result<StravaAuthEntity> {
        return try {
            val currentAuth = getAuth()
                ?: return Result.failure(Exception("Not authenticated"))

            val response = stravaApi.api.refreshAccessToken(
                clientId = StravaConfig.CLIENT_ID,
                clientSecret = StravaConfig.CLIENT_SECRET,
                refreshToken = currentAuth.refreshToken
            )

            if (response.isSuccessful) {
                val tokenResponse = response.body()!!
                val updatedAuth = currentAuth.copy(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresAt = tokenResponse.expiresAt,
                    lastRefreshedAt = System.currentTimeMillis()
                )

                stravaAuthDao.insert(updatedAuth)

                Result.success(updatedAuth)
            } else {
                Result.failure(Exception("Failed to refresh token: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get valid access token (refreshes if expired)
     */
    suspend fun getValidAccessToken(): Result<String> {
        val auth = getAuth()
            ?: return Result.failure(Exception("Not authenticated"))

        // Check if token is expired or will expire soon (5 min buffer)
        if (auth.isExpired(bufferSeconds = 300)) {
            // Refresh token
            val refreshResult = refreshAccessToken()
            return if (refreshResult.isSuccess) {
                Result.success(refreshResult.getOrNull()!!.accessToken)
            } else {
                refreshResult.map { it.accessToken }
            }
        }

        return Result.success(auth.accessToken)
    }

    /**
     * Deauthorize and disconnect Strava
     */
    suspend fun disconnect(): Result<Unit> {
        return try {
            val auth = getAuth()
            if (auth != null) {
                // Revoke access on Strava
                stravaApi.api.deauthorize(
                    accessToken = StravaApiClient.formatAuthHeader(auth.accessToken)
                )

                // Delete from database
                stravaAuthDao.deleteAuth()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            // Still delete locally even if API call fails
            stravaAuthDao.deleteAuth()
            Result.failure(e)
        }
    }

    /**
     * Build Strava OAuth authorization URL
     */
    fun buildAuthUrl(): String {
        return StravaConfig.buildAuthUrl()
    }
}

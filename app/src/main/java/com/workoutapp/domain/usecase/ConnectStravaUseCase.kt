package com.workoutapp.domain.usecase

import com.workoutapp.data.database.entities.StravaAuthEntity
import com.workoutapp.data.repository.StravaAuthRepository
import javax.inject.Inject

/**
 * Use case for connecting to Strava via OAuth
 */
class ConnectStravaUseCase @Inject constructor(
    private val stravaAuthRepository: StravaAuthRepository
) {

    /**
     * Build Strava OAuth authorization URL
     */
    fun buildAuthUrl(): String {
        return stravaAuthRepository.buildAuthUrl()
    }

    /**
     * Handle OAuth callback with authorization code
     */
    suspend fun handleAuthCallback(code: String): Result<StravaAuthEntity> {
        return stravaAuthRepository.exchangeCodeForTokens(code)
    }

    /**
     * Disconnect from Strava
     */
    suspend fun disconnect(): Result<Unit> {
        return stravaAuthRepository.disconnect()
    }

    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean {
        return stravaAuthRepository.isAuthenticated()
    }
}

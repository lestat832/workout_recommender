package com.workoutapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun isOnboardingComplete(): Flow<Boolean>
    suspend fun markOnboardingComplete()
    suspend fun clearOnboardingComplete()
}
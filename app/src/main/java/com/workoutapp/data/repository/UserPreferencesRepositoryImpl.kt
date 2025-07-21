package com.workoutapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.workoutapp.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {
    
    companion object {
        private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
    }
    
    override fun isOnboardingComplete(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[ONBOARDING_COMPLETE_KEY] ?: false
        }
    }
    
    override suspend fun markOnboardingComplete() {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETE_KEY] = true
        }
    }
    
    override suspend fun clearOnboardingComplete() {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETE_KEY] = false
        }
    }
}
package com.workoutapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutapp.data.database.entities.StravaAuthEntity
import com.workoutapp.data.repository.StravaAuthRepository
import com.workoutapp.domain.usecase.ConnectStravaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for Strava authentication
 */
data class StravaAuthUiState(
    val isAuthenticated: Boolean = false,
    val auth: StravaAuthEntity? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val authUrl: String? = null
)

/**
 * ViewModel for Strava authentication flow
 */
@HiltViewModel
class StravaAuthViewModel @Inject constructor(
    private val connectStravaUseCase: ConnectStravaUseCase,
    private val stravaAuthRepository: StravaAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StravaAuthUiState())
    val uiState: StateFlow<StravaAuthUiState> = _uiState.asStateFlow()

    /**
     * Observe authentication state from database
     */
    val authState: StateFlow<StravaAuthEntity?> = stravaAuthRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Update UI state when auth changes
        viewModelScope.launch {
            authState.collect { auth ->
                _uiState.value = _uiState.value.copy(
                    auth = auth,
                    isAuthenticated = auth?.isAuthenticated() == true
                )
            }
        }
    }

    /**
     * Initiate Strava OAuth flow
     */
    fun connectStrava() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val authUrl = connectStravaUseCase.buildAuthUrl()
                _uiState.value = _uiState.value.copy(
                    authUrl = authUrl,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to build auth URL: ${e.message}"
                )
            }
        }
    }

    /**
     * Handle OAuth callback with authorization code
     */
    fun handleAuthCallback(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val result = connectStravaUseCase.handleAuthCallback(code)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    auth = result.getOrNull(),
                    isAuthenticated = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to connect: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Disconnect from Strava
     */
    fun disconnect() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val result = connectStravaUseCase.disconnect()

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    auth = null,
                    isAuthenticated = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to disconnect: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Clear auth URL after opening browser
     */
    fun clearAuthUrl() {
        _uiState.value = _uiState.value.copy(authUrl = null)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

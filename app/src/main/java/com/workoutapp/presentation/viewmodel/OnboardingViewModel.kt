package com.workoutapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutapp.domain.model.Gym
import com.workoutapp.domain.repository.GymRepository
import com.workoutapp.domain.repository.UserPreferencesRepository
import com.workoutapp.domain.usecase.SetDefaultGymUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val gymRepository: GymRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val setDefaultGymUseCase: SetDefaultGymUseCase
) : ViewModel() {

    private val _gyms = MutableStateFlow<List<Gym>>(emptyList())
    val gyms: StateFlow<List<Gym>> = _gyms.asStateFlow()

    private val _selectedGymId = MutableStateFlow<Long?>(null)
    val selectedGymId: StateFlow<Long?> = _selectedGymId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    init {
        viewModelScope.launch {
            val allGyms = gymRepository.getAllGyms()
            _gyms.value = allGyms
            // Pre-select the current default gym
            _selectedGymId.value = allGyms.firstOrNull { it.isDefault }?.id
                ?: allGyms.firstOrNull()?.id
        }
    }

    fun selectGym(gymId: Long) {
        _selectedGymId.value = gymId
    }

    fun selectGymAndComplete() {
        val gymId = _selectedGymId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                setDefaultGymUseCase(gymId)
                userPreferencesRepository.setSelectedGymId(gymId)
                userPreferencesRepository.markOnboardingComplete()
                _isComplete.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

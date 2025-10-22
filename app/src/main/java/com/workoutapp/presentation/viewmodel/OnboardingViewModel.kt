package com.workoutapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutapp.domain.model.EquipmentType
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.Gym
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.UserPreferencesRepository
import com.workoutapp.domain.usecase.CreateGymUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep {
    GYM_SETUP,
    EXERCISE_SELECTION
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val createGymUseCase: CreateGymUseCase
) : ViewModel() {

    private val _currentStep = MutableStateFlow(OnboardingStep.GYM_SETUP)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    // Gym setup state
    private val _gymName = MutableStateFlow("Home Gym")
    val gymName: StateFlow<String> = _gymName.asStateFlow()

    private val _selectedEquipment = MutableStateFlow<Set<String>>(emptySet())
    val selectedEquipment: StateFlow<Set<String>> = _selectedEquipment.asStateFlow()

    // Exercise selection state
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _selectedExercises = MutableStateFlow<Set<String>>(emptySet())
    val selectedExercises: StateFlow<Set<String>> = _selectedExercises.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()
    
    // Gym Setup Methods
    fun setGymName(name: String) {
        _gymName.value = name
    }

    fun toggleEquipmentSelection(equipment: String) {
        _selectedEquipment.value = if (equipment in _selectedEquipment.value) {
            _selectedEquipment.value - equipment
        } else {
            _selectedEquipment.value + equipment
        }
    }

    fun selectAllEquipment() {
        _selectedEquipment.value = EquipmentType.ALL_EQUIPMENT.toSet()
    }

    fun proceedToExerciseSelection() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Create gym with selected equipment
                createGymUseCase(
                    name = _gymName.value,
                    equipmentList = _selectedEquipment.value.toList(),
                    setAsDefault = true
                )

                // Move to exercise selection step
                _currentStep.value = OnboardingStep.EXERCISE_SELECTION
                loadExercises()
            } catch (e: Exception) {
                // Handle error (in a real app, you'd show an error message)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Exercise Selection Methods
    private fun loadExercises() {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exerciseList ->
                // Filter exercises based on selected equipment
                val filteredExercises = exerciseList.filter { exercise ->
                    EquipmentType.canPerformExercise(
                        exercise.equipment,
                        _selectedEquipment.value.toList()
                    )
                }
                _exercises.value = filteredExercises
            }
        }
    }

    fun toggleExerciseSelection(exerciseId: String) {
        _selectedExercises.value = if (exerciseId in _selectedExercises.value) {
            _selectedExercises.value - exerciseId
        } else {
            _selectedExercises.value + exerciseId
        }
    }

    fun selectAllExercises() {
        _selectedExercises.value = _exercises.value.map { it.id }.toSet()
    }

    fun clearAllExercises() {
        _selectedExercises.value = emptySet()
    }

    fun saveSelectedExercises() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                exerciseRepository.setUserExercises(_selectedExercises.value.toList())
                userPreferencesRepository.markOnboardingComplete()
                _isComplete.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }
}
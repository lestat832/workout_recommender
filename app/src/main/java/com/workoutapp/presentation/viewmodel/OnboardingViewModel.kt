package com.workoutapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()
    
    private val _selectedExercises = MutableStateFlow<Set<String>>(emptySet())
    val selectedExercises: StateFlow<Set<String>> = _selectedExercises.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()
    
    init {
        loadExercises()
    }
    
    private fun loadExercises() {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exerciseList ->
                _exercises.value = exerciseList
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
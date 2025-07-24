package com.workoutapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType
import com.workoutapp.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun createExercise(
        name: String,
        muscleGroups: List<MuscleGroup>,
        equipment: String
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                // Validate exercise name doesn't already exist
                val existingExercise = exerciseRepository.getCustomExerciseByName(name)
                if (existingExercise != null) {
                    _uiState.value = UiState.Error("An exercise with this name already exists")
                    return@launch
                }
                
                // Determine workout type based on muscle groups
                val workoutType = determineWorkoutType(muscleGroups)
                
                // Create the exercise
                val exercise = Exercise(
                    id = Exercise.generateCustomId(),
                    name = name.trim(),
                    muscleGroups = muscleGroups,
                    equipment = equipment,
                    category = workoutType,
                    imageUrl = null, // Will use wolf placeholder
                    instructions = emptyList(),
                    isUserCreated = true
                )
                
                // Save to database
                exerciseRepository.createCustomExercise(exercise)
                
                _uiState.value = UiState.Success(exercise)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to create exercise")
            }
        }
    }
    
    private fun determineWorkoutType(muscleGroups: List<MuscleGroup>): WorkoutType {
        // If any PUSH muscle is included, categorize as PUSH
        val pushMuscles = setOf(MuscleGroup.CHEST, MuscleGroup.SHOULDER, MuscleGroup.TRICEP)
        
        return if (muscleGroups.any { it in pushMuscles }) {
            WorkoutType.PUSH
        } else {
            WorkoutType.PULL
        }
    }
    
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val exercise: Exercise) : UiState()
        data class Error(val message: String) : UiState()
    }
}
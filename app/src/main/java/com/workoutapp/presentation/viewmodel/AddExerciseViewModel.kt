package com.workoutapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.WorkoutType
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadAvailableExercises(workoutType: String, currentExerciseIds: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Get all exercises and user's active exercise IDs
                val allExercises = exerciseRepository.getAllExercises().first()
                val activeExerciseIds = exerciseRepository.getActiveUserExercises().first()
                
                // Filter to only user-selected exercises
                val userSelectedExercises = allExercises.filter { exercise ->
                    exercise.id in activeExerciseIds
                }
                
                // Get exercises done in the last week
                val recentExerciseIds = workoutRepository.getExerciseIdsFromLastWeek()
                
                // Filter out current exercises and recent exercises
                // Ensure currentExerciseIds doesn't contain empty strings
                val cleanCurrentExerciseIds = currentExerciseIds.filter { it.isNotBlank() }
                val availableExercises = userSelectedExercises.filter { exercise ->
                    exercise.id !in cleanCurrentExerciseIds && exercise.id !in recentExerciseIds
                }
                
                
                _exercises.value = availableExercises
            } catch (e: Exception) {
                // Handle error - could add error state if needed
                _exercises.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
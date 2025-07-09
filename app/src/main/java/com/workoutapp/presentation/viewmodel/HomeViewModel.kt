package com.workoutapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutStatus
import com.workoutapp.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    
    private val _lastWorkout = MutableStateFlow<Workout?>(null)
    val lastWorkout: StateFlow<Workout?> = _lastWorkout.asStateFlow()
    
    private val _recentWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val recentWorkouts: StateFlow<List<Workout>> = _recentWorkouts.asStateFlow()
    
    init {
        loadLastWorkout()
        loadRecentWorkouts()
    }
    
    private fun loadLastWorkout() {
        viewModelScope.launch {
            _lastWorkout.value = workoutRepository.getLastWorkout()
        }
    }
    
    private fun loadRecentWorkouts() {
        viewModelScope.launch {
            workoutRepository.getWorkoutsByStatus(WorkoutStatus.COMPLETED).collect { workouts ->
                // Load full workout details for each workout
                val detailedWorkouts = workouts.take(5).map { workout ->
                    workoutRepository.getWorkoutById(workout.id) ?: workout
                }
                _recentWorkouts.value = detailedWorkouts
            }
        }
    }
    
    fun calculateTotalWeight(workout: Workout): Float {
        return workout.exercises.sumOf { exercise ->
            exercise.sets.sumOf { set ->
                if (set.completed) (set.weight * set.reps).toDouble() else 0.0
            }
        }.toFloat()
    }
}
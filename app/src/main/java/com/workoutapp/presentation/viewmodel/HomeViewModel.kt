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
                _recentWorkouts.value = workouts.take(5)
            }
        }
    }
}
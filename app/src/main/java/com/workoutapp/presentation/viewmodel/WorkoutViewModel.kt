package com.workoutapp.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workoutapp.domain.model.*
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.WorkoutRepository
import com.workoutapp.domain.usecase.GenerateWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    application: Application,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val generateWorkoutUseCase: GenerateWorkoutUseCase
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()
    
    private var currentWorkout: Workout? = null
    
    init {
        startWorkout()
    }
    
    private fun startWorkout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val exercises = generateWorkoutUseCase()
                if (exercises.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No exercises available. Please select more exercises."
                    )
                    return@launch
                }
                
                val workoutId = UUID.randomUUID().toString()
                val lastWorkout = workoutRepository.getLastWorkout()
                val workoutType = if (lastWorkout?.type == WorkoutType.PUSH) {
                    WorkoutType.PULL
                } else {
                    WorkoutType.PUSH
                }
                
                // Get date offset from shared preferences for testing
                val prefs = getApplication<Application>().getSharedPreferences("debug_prefs", android.content.Context.MODE_PRIVATE)
                val dateOffset = prefs.getInt("date_offset", 0)
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, dateOffset)
                
                val workout = Workout(
                    id = workoutId,
                    date = calendar.time,
                    type = workoutType,
                    status = WorkoutStatus.IN_PROGRESS,
                    exercises = exercises.map { exercise ->
                        WorkoutExercise(
                            id = UUID.randomUUID().toString(),
                            workoutId = workoutId,
                            exercise = exercise,
                            sets = listOf(com.workoutapp.domain.model.Set(reps = 0, weight = 0f, completed = false))
                        )
                    }
                )
                
                workoutRepository.createWorkout(workout)
                currentWorkout = workout
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    exercises = workout.exercises
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to generate workout"
                )
            }
        }
    }
    
    fun addSet(exerciseId: String) {
        val exercises = _uiState.value.exercises.map { exercise ->
            if (exercise.id == exerciseId) {
                exercise.copy(
                    sets = exercise.sets + com.workoutapp.domain.model.Set(reps = 0, weight = 0f, completed = false)
                )
            } else {
                exercise
            }
        }
        _uiState.value = _uiState.value.copy(exercises = exercises)
    }
    
    fun removeSet(exerciseId: String, setIndex: Int) {
        val exercises = _uiState.value.exercises.map { exercise ->
            if (exercise.id == exerciseId && exercise.sets.size > 1) {
                exercise.copy(
                    sets = exercise.sets.filterIndexed { index, _ -> index != setIndex }
                )
            } else {
                exercise
            }
        }
        _uiState.value = _uiState.value.copy(exercises = exercises)
    }
    
    fun updateSet(exerciseId: String, setIndex: Int, reps: Int, weight: Float) {
        val exercises = _uiState.value.exercises.map { exercise ->
            if (exercise.id == exerciseId) {
                exercise.copy(
                    sets = exercise.sets.mapIndexed { index, set ->
                        if (index == setIndex) {
                            set.copy(reps = reps, weight = weight, completed = reps > 0)
                        } else {
                            set
                        }
                    }
                )
            } else {
                exercise
            }
        }
        _uiState.value = _uiState.value.copy(exercises = exercises)
    }
    
    fun shuffleExercise(exerciseId: String) {
        viewModelScope.launch {
            val currentExercise = _uiState.value.exercises.find { it.id == exerciseId } ?: return@launch
            val workoutType = currentWorkout?.type ?: return@launch
            
            // Get all selected exercises of the same type
            val selectedExercises = exerciseRepository.getUserActiveExercisesByType(workoutType)
            
            // Get exercises done in the last week
            val recentExerciseIds = workoutRepository.getExerciseIdsFromLastWeek()
            
            // Filter out current exercises and recent exercises
            val currentExerciseIds = _uiState.value.exercises.map { it.exercise.id }
            val availableExercises = selectedExercises.filter { exercise ->
                exercise.id !in currentExerciseIds && exercise.id !in recentExerciseIds
            }
            
            if (availableExercises.isEmpty()) {
                // If no alternatives, try without recent exercise filter
                val lessRestrictiveExercises = selectedExercises.filter { exercise ->
                    exercise.id !in currentExerciseIds
                }
                
                if (lessRestrictiveExercises.isNotEmpty()) {
                    val newExercise = lessRestrictiveExercises.random()
                    replaceExercise(exerciseId, newExercise)
                }
                // If still no alternatives, do nothing
            } else {
                val newExercise = availableExercises.random()
                replaceExercise(exerciseId, newExercise)
            }
        }
    }
    
    private fun replaceExercise(oldExerciseId: String, newExercise: Exercise) {
        val exercises = _uiState.value.exercises.map { workoutExercise ->
            if (workoutExercise.id == oldExerciseId) {
                WorkoutExercise(
                    id = UUID.randomUUID().toString(),
                    workoutId = workoutExercise.workoutId,
                    exercise = newExercise,
                    sets = listOf(com.workoutapp.domain.model.Set(reps = 0, weight = 0f, completed = false))
                )
            } else {
                workoutExercise
            }
        }
        _uiState.value = _uiState.value.copy(exercises = exercises)
    }
    
    fun completeWorkout() {
        viewModelScope.launch {
            currentWorkout?.let { workout ->
                // Save all workout exercises
                _uiState.value.exercises.forEach { exercise ->
                    workoutRepository.addExerciseToWorkout(workout.id, exercise)
                }
                
                // Update workout status
                val completedWorkout = workout.copy(
                    status = WorkoutStatus.COMPLETED,
                    exercises = _uiState.value.exercises
                )
                workoutRepository.updateWorkout(completedWorkout)
                
                _uiState.value = _uiState.value.copy(isCompleted = true)
            }
        }
    }
}

data class WorkoutUiState(
    val isLoading: Boolean = false,
    val exercises: List<WorkoutExercise> = emptyList(),
    val error: String? = null,
    val isCompleted: Boolean = false
)
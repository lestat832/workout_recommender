package com.workoutapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutStatus
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.WorkoutRepository
import com.workoutapp.domain.usecase.ImportDebugDataUseCase
import com.workoutapp.domain.usecase.ImportResult
import com.workoutapp.domain.usecase.ImportWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val importWorkoutUseCase: ImportWorkoutUseCase,
    private val importDebugDataUseCase: ImportDebugDataUseCase
) : ViewModel() {
    
    private val _lastWorkout = MutableStateFlow<Workout?>(null)
    val lastWorkout: StateFlow<Workout?> = _lastWorkout.asStateFlow()
    
    private val _recentWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val recentWorkouts: StateFlow<List<Workout>> = _recentWorkouts.asStateFlow()
    
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()
    
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
                // Load full workout details for each workout - show more imported workouts
                val detailedWorkouts = workouts.take(50).map { workout ->
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
    
    fun importWorkouts(csvContent: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            
            try {
                val result = importWorkoutUseCase.importFromCsvContent(csvContent)
                _importState.value = ImportState.Success(result)
                
                // Reload workouts after import
                loadLastWorkout()
                loadRecentWorkouts()
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    fun resetImportState() {
        _importState.value = ImportState.Idle
    }
    
    fun resetDebugDataImport() {
        importDebugDataUseCase.resetDebugDataImport()
    }
    
    fun isDebugDataImported(): Boolean {
        return importDebugDataUseCase.isDebugDataImported()
    }
    
    fun reimportDebugData() {
        viewModelScope.launch {
            // Reset the flag first
            importDebugDataUseCase.resetDebugDataImport()
            
            // Then re-import
            importDebugDataUseCase()
            
            // Reload workouts
            loadLastWorkout()
            loadRecentWorkouts()
        }
    }
    
    fun importMarcWorkouts() {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            
            try {
                // Reset the import flag to allow re-import
                importDebugDataUseCase.resetDebugDataImport()
                
                // Import Marc's workouts from embedded CSV using manual import
                importDebugDataUseCase.importManually()
                
                // Show success
                _importState.value = ImportState.Success(
                    ImportResult(
                        totalWorkouts = 133,
                        importedWorkouts = 133,
                        newExercises = 0,
                        mappedExercises = 0,
                        errors = emptyList()
                    )
                )
                
                // Reload workouts
                loadLastWorkout()
                loadRecentWorkouts()
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Failed to import workouts")
            }
        }
    }
}

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val result: ImportResult) : ImportState()
    data class Error(val message: String) : ImportState()
}
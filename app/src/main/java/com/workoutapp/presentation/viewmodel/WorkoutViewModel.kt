package com.workoutapp.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.workoutapp.domain.model.*
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.WorkoutRepository
import com.workoutapp.domain.usecase.GenerateWorkoutUseCase
import com.workoutapp.domain.usecase.ProfileComputerUseCase
import com.workoutapp.data.sync.StravaSyncManager
import com.workoutapp.domain.usecase.StrengthSetPrescriber
import com.workoutapp.domain.usecase.toWorkoutPrescription
import com.workoutapp.domain.repository.TrainingProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val generateWorkoutUseCase: GenerateWorkoutUseCase,
    private val profileComputerUseCase: ProfileComputerUseCase,
    private val profileRepository: TrainingProfileRepository,
    private val stravaSyncManager: StravaSyncManager
) : AndroidViewModel(application) {

    private val gymId: Long? = savedStateHandle["gymId"]

    // Optional skip-button override. Null for the normal flow; populated when
    // the user taps "Skip" on the NextWorkoutCard and nav passes ?type=PUSH/PULL.
    private val typeOverride: WorkoutType? =
        savedStateHandle.get<String>("type")
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { WorkoutType.valueOf(it) }.getOrNull() }

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private var currentWorkout: Workout? = null
    private var workoutStartTime: Long = System.currentTimeMillis()

    init {
        startWorkout()
    }

    private fun startWorkout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Check for an existing IN_PROGRESS strength workout at this gym
                val existingWorkout = gymId?.let { workoutRepository.getInProgressStrengthWorkout(it) }
                if (existingWorkout != null && existingWorkout.exercises.isNotEmpty()) {
                    currentWorkout = existingWorkout
                    workoutStartTime = existingWorkout.date.time
                    val prescriptions = buildPrescriptions(existingWorkout.exercises)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        exercises = existingWorkout.exercises,
                        prescriptions = prescriptions
                    )
                    return@launch
                }

                val generated = generateWorkoutUseCase(gymId, typeOverride)
                if (generated.exercises.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No exercises available. Please select more exercises."
                    )
                    return@launch
                }

                val workoutId = UUID.randomUUID().toString()
                val workoutType = generated.type

                val prefs = getApplication<Application>().getSharedPreferences("debug_prefs", android.content.Context.MODE_PRIVATE)
                val dateOffset = prefs.getInt("date_offset", 0)
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, dateOffset)

                val workout = Workout(
                    id = workoutId,
                    date = calendar.time,
                    type = workoutType,
                    status = WorkoutStatus.IN_PROGRESS,
                    gymId = gymId,
                    exercises = generated.exercises.map { exercise ->
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

                val prescriptions = buildPrescriptions(workout.exercises)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    exercises = workout.exercises,
                    prescriptions = prescriptions
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
                // Find the last completed set to copy its values
                val lastCompletedSet = exercise.sets.lastOrNull { it.completed }
                val newSet = if (lastCompletedSet != null) {
                    // Copy weight and reps from last completed set
                    com.workoutapp.domain.model.Set(
                        reps = lastCompletedSet.reps,
                        weight = lastCompletedSet.weight,
                        completed = false
                    )
                } else {
                    // No completed sets, use empty values
                    com.workoutapp.domain.model.Set(reps = 0, weight = 0f, completed = false)
                }
                
                exercise.copy(sets = exercise.sets + newSet)
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
                            set.copy(reps = reps, weight = weight)
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
    
    fun toggleSetCompletion(exerciseId: String, setIndex: Int) {
        val exercises = _uiState.value.exercises.map { exercise ->
            if (exercise.id == exerciseId) {
                exercise.copy(
                    sets = exercise.sets.mapIndexed { index, set ->
                        if (index == setIndex) {
                            set.copy(completed = !set.completed)
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
            val currentMuscleGroups = currentExercise.exercise.muscleGroups
            
            // Get all exercises that target the same muscle groups
            val allExercises = exerciseRepository.getAllExercises().firstOrNull() ?: emptyList()
            val activeExerciseIds = exerciseRepository.getActiveUserExercises().firstOrNull() ?: emptyList()
            
            // Filter exercises that share at least one muscle group with current exercise
            val sameMuscleFroupExercises = allExercises.filter { exercise ->
                exercise.id in activeExerciseIds && 
                exercise.muscleGroups.any { it in currentMuscleGroups }
            }
            
            // Get exercises done in the last week
            val recentExerciseIds = workoutRepository.getExerciseIdsFromLastWeek()
            
            // Filter out current exercises and recent exercises
            val currentExerciseIds = _uiState.value.exercises.map { it.exercise.id }
            val availableExercises = sameMuscleFroupExercises.filter { exercise ->
                exercise.id !in currentExerciseIds && exercise.id !in recentExerciseIds
            }
            
            if (availableExercises.isEmpty()) {
                // If no alternatives, try without recent exercise filter
                val lessRestrictiveExercises = sameMuscleFroupExercises.filter { exercise ->
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
    
    fun removeExercise(exerciseId: String) {
        val exercises = _uiState.value.exercises.filter { it.id != exerciseId }
        _uiState.value = _uiState.value.copy(exercises = exercises)
    }
    
    fun addExerciseToWorkout(exercise: Exercise) {
        val workoutId = currentWorkout?.id ?: return
        
        val newWorkoutExercise = WorkoutExercise(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            exercise = exercise,
            sets = listOf(com.workoutapp.domain.model.Set(reps = 0, weight = 0f, completed = false))
        )
        
        val exercises = _uiState.value.exercises + newWorkoutExercise
        _uiState.value = _uiState.value.copy(exercises = exercises)
    }
    
    /**
     * For each exercise in the freshly generated workout, try the profile-aware
     * prescriber first (per-set weights based on loading pattern). Falls back to
     * the legacy history-based prescriber for exercises without a profile.
     */
    private suspend fun buildPrescriptions(
        exercises: List<WorkoutExercise>
    ): Map<String, WorkoutPrescription> {
        if (exercises.isEmpty()) return emptyMap()

        // Load profiles for all exercises in this workout
        val exerciseIds = exercises.map { it.exercise.id }
        val profiles = profileRepository.getExerciseProfiles(exerciseIds)
            .associateBy { it.exerciseId }

        // Fallback: load history for exercises without profiles
        val needsHistory = exercises.filter { profiles[it.exercise.id]?.strengthSessionCount ?: 0 < 2 }
        val fullCandidates = if (needsHistory.isNotEmpty()) {
            val completed = workoutRepository
                .getWorkoutsByStatus(WorkoutStatus.COMPLETED)
                .firstOrNull()
                .orEmpty()
            completed.take(HISTORY_LOOKBACK_WORKOUTS).mapNotNull {
                workoutRepository.getWorkoutById(it.id)
            }
        } else emptyList()

        return exercises.mapIndexed { index, workoutExercise ->
            val profile = profiles[workoutExercise.exercise.id]

            val prescription = if (profile != null && profile.strengthSessionCount >= 2) {
                // Profile-aware: per-set prescriptions with loading pattern
                StrengthSetPrescriber.prescribeFromProfile(
                    positionInWorkout = index,
                    profile = profile
                )
            } else {
                // Fallback: legacy history-based
                val targetId = workoutExercise.exercise.id
                val history = fullCandidates
                    .mapNotNull { w -> w.exercises.firstOrNull { it.exercise.id == targetId } }
                    .take(2)
                val legacy = StrengthSetPrescriber.prescribe(
                    positionInWorkout = index,
                    history = history
                )
                // Wrap legacy prescription into WorkoutPrescription
                legacy.toWorkoutPrescription()
            }

            workoutExercise.id to prescription
        }.toMap()
    }

    fun getCurrentWorkoutType(): WorkoutType? {
        return currentWorkout?.type
    }
    
    fun getCurrentExerciseIds(): List<String> {
        return _uiState.value.exercises.map { it.exercise.id }
    }
    
    fun cancelWorkout() {
        viewModelScope.launch {
            currentWorkout?.let { workout ->
                // Delete the workout entirely
                workoutRepository.deleteWorkout(workout.id)
                _uiState.value = _uiState.value.copy(isCompleted = true)
            }
        }
    }
    
    fun saveWorkoutProgress() {
        viewModelScope.launch {
            currentWorkout?.let { workout ->
                // Save all workout exercises with current progress
                _uiState.value.exercises.forEach { exercise ->
                    workoutRepository.addExerciseToWorkout(workout.id, exercise)
                }
                
                // Update workout status to incomplete
                val incompleteWorkout = workout.copy(
                    status = WorkoutStatus.INCOMPLETE,
                    exercises = _uiState.value.exercises
                )
                workoutRepository.updateWorkout(incompleteWorkout)
                
                _uiState.value = _uiState.value.copy(isCompleted = true)
            }
        }
    }
    
    fun completeWorkout() {
        viewModelScope.launch {
            currentWorkout?.let { workout ->
                // Save all workout exercises
                _uiState.value.exercises.forEach { exercise ->
                    workoutRepository.addExerciseToWorkout(workout.id, exercise)
                }

                // Compute session duration from start time
                val durationMin = ((System.currentTimeMillis() - workoutStartTime) / 60000).toInt()

                // Update workout status
                val completedWorkout = workout.copy(
                    status = WorkoutStatus.COMPLETED,
                    exercises = _uiState.value.exercises,
                    durationMinutes = durationMin
                )
                workoutRepository.updateWorkout(completedWorkout)

                // Update training profile
                profileComputerUseCase.updateAfterWorkout(completedWorkout.id)
                stravaSyncManager.queueWorkout(completedWorkout.id)

                _uiState.value = _uiState.value.copy(isCompleted = true)
            }
        }
    }
}

data class WorkoutUiState(
    val isLoading: Boolean = false,
    val exercises: List<WorkoutExercise> = emptyList(),
    val prescriptions: Map<String, WorkoutPrescription> = emptyMap(),
    val error: String? = null,
    val isCompleted: Boolean = false
)

// Number of most-recent completed workouts to scan when building the
// per-exercise history window. Over-fetch beyond the 2-session minimum so
// sessions that didn't include a given exercise don't starve the lookup.
private const val HISTORY_LOOKBACK_WORKOUTS = 20
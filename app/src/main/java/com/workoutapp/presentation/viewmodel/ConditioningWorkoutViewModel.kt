package com.workoutapp.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.workoutapp.domain.model.Set
import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutExercise
import com.workoutapp.domain.model.WorkoutFormat
import com.workoutapp.domain.model.WorkoutStatus
import com.workoutapp.domain.model.WorkoutType
import com.workoutapp.domain.repository.WorkoutRepository
import com.workoutapp.domain.usecase.DeleteWorkoutUseCase
import com.workoutapp.domain.usecase.GenerateConditioningWorkoutUseCase
import com.workoutapp.domain.usecase.GenerateConditioningWorkoutUseCase.Companion.AMRAP_DURATION_MINUTES
import com.workoutapp.domain.usecase.GenerateConditioningWorkoutUseCase.Companion.EMOM_DURATION_MINUTES
import com.workoutapp.domain.usecase.ProfileComputerUseCase
import com.workoutapp.data.database.HomeGymMovementCatalog
import com.workoutapp.data.sync.StravaSyncManager
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.usecase.RepPrescriber
import android.util.Log
import com.workoutapp.domain.model.CompletedWorkoutSummary
import com.workoutapp.domain.usecase.FatigueAwareness
import java.util.Date
import java.util.concurrent.TimeUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ConditioningWorkoutViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val generateConditioningWorkoutUseCase: GenerateConditioningWorkoutUseCase,
    private val profileComputerUseCase: ProfileComputerUseCase,
    private val stravaSyncManager: StravaSyncManager,
    private val deleteWorkoutUseCase: DeleteWorkoutUseCase
) : AndroidViewModel(application) {

    private val gymId: Long? = savedStateHandle["gymId"]

    // Optional skip-button override. Null for the normal flow; populated when
    // the user taps "Skip" on the NextWorkoutCard and nav passes ?format=EMOM/AMRAP.
    private val formatOverride: WorkoutFormat? =
        savedStateHandle.get<String>("format")
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { WorkoutFormat.valueOf(it) }.getOrNull() }

    private val _uiState = MutableStateFlow(ConditioningUiState())
    val uiState: StateFlow<ConditioningUiState> = _uiState.asStateFlow()

    private var currentWorkout: Workout? = null

    init {
        generateWorkout()
    }

    private fun generateWorkout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val id = gymId ?: error("gymId missing from SavedStateHandle")

                // Check for stale conditioning workout — will delete after
                // successful generation to avoid data loss on failed generation.
                val stale = workoutRepository.getInProgressConditioningWorkout(id)

                val generated = generateConditioningWorkoutUseCase(id, formatOverride)

                // Generation succeeded — now safe to clean up stale row
                if (stale != null) {
                    workoutRepository.deleteWorkout(stale.id)
                }

                val workoutId = UUID.randomUUID().toString()
                val calendar = Calendar.getInstance().apply { time = effectiveNow() }

                val workout = Workout(
                    id = workoutId,
                    date = calendar.time,
                    type = WorkoutType.PULL,
                    status = WorkoutStatus.IN_PROGRESS,
                    gymId = id,
                    format = generated.format,
                    durationMinutes = generated.durationMinutes,
                    completedRounds = null,
                    exercises = run {
                        val sessionIds = generated.exercises.map { it.id }
                        generated.exercises.map { exercise ->
                            WorkoutExercise(
                                id = UUID.randomUUID().toString(),
                                workoutId = workoutId,
                                exercise = exercise,
                                sets = listOf(Set(reps = 0, weight = 0f, completed = true)),
                                prescription = RepPrescriber.prescribe(
                                    exerciseId = exercise.id,
                                    format = generated.format,
                                    sessionIds = sessionIds
                                )
                            )
                        }
                    }
                )

                workoutRepository.createWorkout(workout)
                workout.exercises.forEach { ex ->
                    workoutRepository.addExerciseToWorkout(workout.id, ex)
                }
                currentWorkout = workout

                val fatigueWarning = runCatching { computeFatigueWarning(workout.exercises) }.getOrNull()

                _uiState.value = ConditioningUiState(
                    isLoading = false,
                    isPreview = true,
                    format = generated.format,
                    durationMinutes = generated.durationMinutes,
                    exercises = workout.exercises,
                    coachNote = buildCoachNote(generated.format, workout.exercises),
                    elapsedSeconds = 0,
                    rounds = 0,
                    fatigueWarning = fatigueWarning
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to generate workout"
                )
            }
        }
    }

    private fun runTimer() {
        viewModelScope.launch {
            val durationSec = (_uiState.value.durationMinutes ?: 20) * 60
            while (!_uiState.value.isCompleted && _uiState.value.elapsedSeconds < durationSec) {
                delay(1000)
                if (!_uiState.value.isPaused) {
                    val next = _uiState.value.elapsedSeconds + 1
                    val stationIndex = if (_uiState.value.format == WorkoutFormat.EMOM && _uiState.value.exercises.isNotEmpty()) {
                        (next / 60) % _uiState.value.exercises.size
                    } else -1
                    _uiState.value = _uiState.value.copy(
                        elapsedSeconds = next,
                        currentStationIndex = stationIndex
                    )
                }
            }
        }
    }

    fun beginWorkout() {
        _uiState.value = _uiState.value.copy(isPreview = false)
        runTimer()
    }

    fun shuffleExercise(exerciseId: String) {
        viewModelScope.launch {
            val current = _uiState.value.exercises.find { it.id == exerciseId } ?: return@launch
            val movement = HomeGymMovementCatalog.byId(current.exercise.id) ?: return@launch

            val byBucket = HomeGymMovementCatalog.byBucket()
            val pool = when {
                movement.bucket == HomeGymMovementCatalog.Bucket.CORE ||
                    movement.bucket == HomeGymMovementCatalog.Bucket.CONDITIONING_BODYWEIGHT ->
                    (byBucket[HomeGymMovementCatalog.Bucket.CORE].orEmpty() +
                        byBucket[HomeGymMovementCatalog.Bucket.CONDITIONING_BODYWEIGHT].orEmpty())
                else -> byBucket[movement.bucket].orEmpty()
            }

            val currentIds = _uiState.value.exercises.map { it.exercise.id }.toSet()
            val trxCount = _uiState.value.exercises.count { it.exercise.equipment == "Suspension Trainer" }
            val candidates = pool.filter { m ->
                m.id !in currentIds &&
                    (trxCount < 2 || m.equipment != "Suspension Trainer")
            }
            if (candidates.isEmpty()) return@launch

            val newMovement = candidates.random()
            val newExercise = exerciseRepository.getExerciseById(newMovement.id) ?: return@launch

            val updatedSessionIds = _uiState.value.exercises.map {
                if (it.id == exerciseId) newExercise.id else it.exercise.id
            }
            val prescription = RepPrescriber.prescribe(
                exerciseId = newExercise.id,
                format = _uiState.value.format,
                sessionIds = updatedSessionIds
            )

            val replacement = current.copy(
                exercise = newExercise,
                sets = listOf(Set(reps = 0, weight = 0f, completed = true)),
                prescription = prescription
            )

            val updatedExercises = _uiState.value.exercises.map {
                if (it.id == exerciseId) replacement else it
            }
            _uiState.value = _uiState.value.copy(
                exercises = updatedExercises,
                coachNote = buildCoachNote(_uiState.value.format, updatedExercises)
            )

            // Update the existing row in-place (same ID, different exercise)
            currentWorkout = currentWorkout?.copy(exercises = updatedExercises)
            workoutRepository.updateWorkoutExercise(replacement)
        }
    }

    fun togglePause() {
        _uiState.value = _uiState.value.copy(isPaused = !_uiState.value.isPaused)
    }

    fun incrementRound() {
        _uiState.value = _uiState.value.copy(rounds = _uiState.value.rounds + 1)
    }

    private fun buildCoachNote(format: WorkoutFormat, exercises: List<WorkoutExercise>): String {
        val exerciseNames = exercises.map { it.exercise.name }
        return when (format) {
            WorkoutFormat.EMOM -> {
                val duration = EMOM_DURATION_MINUTES
                val stations = exercises.size
                val rounds = duration / stations
                "Every minute on the minute for $duration minutes. " +
                    "$stations stations, $rounds rounds each. " +
                    "Work for 40-45 seconds, rest the remainder. " +
                    "Follows legs \u2192 pull \u2192 push \u2192 core sequencing " +
                    "to spread the load across muscle groups and keep transitions smooth."
            }
            WorkoutFormat.AMRAP -> {
                val duration = AMRAP_DURATION_MINUTES
                "As many rounds as possible in $duration minutes. " +
                    "Cycle through all ${exercises.size} stations continuously. " +
                    "Start with ${exerciseNames.firstOrNull() ?: "cardio"} to elevate heart rate, " +
                    "then alternate strength and core work. " +
                    "Pace yourself early — the goal is consistent output, not a fast start that fades."
            }
            else -> ""
        }
    }

    private fun effectiveNow(): Date {
        val prefs = getApplication<Application>().getSharedPreferences("debug_prefs", android.content.Context.MODE_PRIVATE)
        val dateOffset = prefs.getInt("date_offset", 0)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dateOffset)
        return cal.time
    }

    private suspend fun computeFatigueWarning(exercises: List<WorkoutExercise>): String? {
        if (exercises.isEmpty()) return null
        val now = effectiveNow()
        val since = Date(now.time - TimeUnit.HOURS.toMillis(FatigueAwareness.OVERLAP_WINDOW_HOURS))
        val recent = workoutRepository.getCompletedWorkoutSummariesSince(since)
        val plannedMuscles = exercises.flatMap { it.exercise.muscleGroups }.toSet()
        val warning = FatigueAwareness.checkMuscleOverlap(
            plannedMuscleGroups = plannedMuscles,
            recentWorkouts = recent,
            now = now
        )
        if (warning != null) {
            Log.d("FortisLupus", "Fatigue overlap (conditioning): $warning")
        }
        return warning
    }

    fun cancelWorkout() {
        viewModelScope.launch {
            currentWorkout?.let { workout ->
                deleteWorkoutUseCase(workout.id)
            }
        }
    }

    fun completeWorkout() {
        viewModelScope.launch {
            val workout = currentWorkout ?: return@launch
            val finalRounds = when (_uiState.value.format) {
                WorkoutFormat.AMRAP -> _uiState.value.rounds
                WorkoutFormat.EMOM -> {
                    val stations = _uiState.value.exercises.size
                    if (stations > 0) {
                        // Rounds from actual elapsed time, not planned duration
                        val elapsedMinutes = _uiState.value.elapsedSeconds / 60
                        elapsedMinutes / stations
                    } else null
                }
                else -> null
            }
            val completed = workout.copy(
                status = WorkoutStatus.COMPLETED,
                completedRounds = finalRounds
            )
            workoutRepository.updateWorkout(completed)
            profileComputerUseCase.updateAfterWorkout(completed.id)
            stravaSyncManager.queueWorkout(completed.id)
            _uiState.value = _uiState.value.copy(isCompleted = true)
        }
    }
}

data class ConditioningUiState(
    val isLoading: Boolean = false,
    val isPreview: Boolean = true,
    val isPaused: Boolean = false,
    val format: WorkoutFormat = WorkoutFormat.EMOM,
    val durationMinutes: Int? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
    val coachNote: String = "",
    val elapsedSeconds: Int = 0,
    val rounds: Int = 0,
    val currentStationIndex: Int = -1,
    val error: String? = null,
    val isCompleted: Boolean = false,
    val fatigueWarning: String? = null
)

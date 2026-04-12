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
import com.workoutapp.domain.usecase.GenerateConditioningWorkoutUseCase
import com.workoutapp.domain.usecase.ProfileComputerUseCase
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
    private val generateConditioningWorkoutUseCase: GenerateConditioningWorkoutUseCase,
    private val profileComputerUseCase: ProfileComputerUseCase
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
        startWorkout()
    }

    private fun startWorkout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val id = gymId ?: error("gymId missing from SavedStateHandle")
                val generated = generateConditioningWorkoutUseCase(id, formatOverride)

                val workoutId = UUID.randomUUID().toString()
                // Honor the existing debug date offset pattern used by the
                // strength workout flow so the +1 Day debug menu still works.
                val prefs = getApplication<Application>().getSharedPreferences(
                    "debug_prefs",
                    android.content.Context.MODE_PRIVATE
                )
                val dateOffset = prefs.getInt("date_offset", 0)
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, dateOffset)

                // Conditioning workouts persist with a nominal WorkoutType to
                // satisfy the non-null column; the format field is what actually
                // drives rendering. PULL is an arbitrary choice that keeps the
                // row loadable by existing queries.
                val workout = Workout(
                    id = workoutId,
                    date = calendar.time,
                    type = WorkoutType.PULL,
                    status = WorkoutStatus.IN_PROGRESS,
                    gymId = id,
                    format = generated.format,
                    durationMinutes = generated.durationMinutes,
                    completedRounds = null,
                    exercises = generated.exercises.map { exercise ->
                        WorkoutExercise(
                            id = UUID.randomUUID().toString(),
                            workoutId = workoutId,
                            exercise = exercise,
                            sets = listOf(Set(reps = 0, weight = 0f, completed = true))
                        )
                    }
                )

                workoutRepository.createWorkout(workout)
                // Persist the exercise rows immediately so dedup queries and
                // Pack History can see them even if the session is abandoned.
                workout.exercises.forEach { ex ->
                    workoutRepository.addExerciseToWorkout(workout.id, ex)
                }
                currentWorkout = workout

                _uiState.value = ConditioningUiState(
                    isLoading = false,
                    format = generated.format,
                    durationMinutes = generated.durationMinutes,
                    exercises = workout.exercises,
                    elapsedSeconds = 0,
                    rounds = 0
                )
                runTimer()
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
                val next = _uiState.value.elapsedSeconds + 1
                _uiState.value = _uiState.value.copy(elapsedSeconds = next)
            }
        }
    }

    fun incrementRound() {
        _uiState.value = _uiState.value.copy(rounds = _uiState.value.rounds + 1)
    }

    fun completeWorkout() {
        viewModelScope.launch {
            val workout = currentWorkout ?: return@launch
            val finalRounds = if (_uiState.value.format == WorkoutFormat.AMRAP) {
                _uiState.value.rounds
            } else {
                null
            }
            val completed = workout.copy(
                status = WorkoutStatus.COMPLETED,
                completedRounds = finalRounds
            )
            workoutRepository.updateWorkout(completed)
            profileComputerUseCase.updateAfterWorkout(completed.id)
            _uiState.value = _uiState.value.copy(isCompleted = true)
        }
    }
}

data class ConditioningUiState(
    val isLoading: Boolean = false,
    val format: WorkoutFormat = WorkoutFormat.EMOM,
    val durationMinutes: Int? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
    val elapsedSeconds: Int = 0,
    val rounds: Int = 0,
    val error: String? = null,
    val isCompleted: Boolean = false
)

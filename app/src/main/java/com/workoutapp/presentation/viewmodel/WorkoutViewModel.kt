package com.workoutapp.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.workoutapp.domain.model.*
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.GymRepository
import com.workoutapp.domain.repository.WorkoutRepository
import android.util.Log
import com.workoutapp.domain.usecase.DeleteWorkoutUseCase
import com.workoutapp.domain.usecase.FatigueAwareness
import com.workoutapp.domain.usecase.GenerateWorkoutUseCase
import com.workoutapp.domain.usecase.ProfileComputerUseCase
import com.workoutapp.data.sync.StravaSyncManager
import com.workoutapp.domain.usecase.StrengthSetPrescriber
import com.workoutapp.domain.usecase.toWorkoutPrescription
import java.util.concurrent.TimeUnit
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
    private val stravaSyncManager: StravaSyncManager,
    private val deleteWorkoutUseCase: DeleteWorkoutUseCase,
    private val blockStateRepository: com.workoutapp.domain.repository.BlockStateRepository,
    private val gymRepository: GymRepository
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
    private var blockState: com.workoutapp.domain.usecase.BlockPeriodization.State? = null
    private var workoutStartTime: Long = System.currentTimeMillis()

    // Per-slot history of recent shuffle picks. Keyed by WorkoutExercise.id (stable across
    // swaps after replaceExercise preserves the slot UUID). Used to bias the candidate pool
    // away from exercises that just cycled through so repeated taps actually vary.
    private val shuffleMemory: MutableMap<String, ArrayDeque<String>> = mutableMapOf()

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
                    // Use "now" not existingWorkout.date: warning reflects current-moment
                    // recovery state, matching when the user is actually training.
                    val fatigueWarning = runCatching { computeFatigueWarning(existingWorkout.exercises) }.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        exercises = existingWorkout.exercises,
                        prescriptions = prescriptions,
                        fatigueWarning = fatigueWarning
                    )
                    return@launch
                }

                val generated = generateWorkoutUseCase(gymId, typeOverride)
                blockState = generated.blockState
                if (generated.exercises.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No exercises available. Please select more exercises."
                    )
                    return@launch
                }

                val workoutId = UUID.randomUUID().toString()
                val workoutType = generated.type

                val calendar = Calendar.getInstance().apply { time = effectiveNow() }

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
                // Persist exercises immediately so the in-progress restore path at
                // startWorkout() can recover the workout even if the process is
                // killed before the user interacts. Without this, createWorkout
                // writes only the parent row and restore fails its exercises
                // non-empty check, forcing a fresh generation.
                workoutRepository.replaceExercisesForWorkout(workout.id, workout.exercises)
                currentWorkout = workout

                val prescriptions = buildPrescriptions(workout.exercises)
                val fatigueWarning = runCatching { computeFatigueWarning(workout.exercises) }.getOrNull()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    exercises = workout.exercises,
                    prescriptions = prescriptions,
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
        autosaveProgress()
    }

    fun removeSet(exerciseId: String, setIndex: Int) {
        var mutated = false
        val exercises = _uiState.value.exercises.map { exercise ->
            if (exercise.id == exerciseId && exercise.sets.size > 1) {
                mutated = true
                exercise.copy(
                    sets = exercise.sets.filterIndexed { index, _ -> index != setIndex }
                )
            } else {
                exercise
            }
        }
        _uiState.value = _uiState.value.copy(exercises = exercises)
        if (mutated) autosaveProgress()
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
        autosaveProgress()
    }

    /**
     * Phase 4a: record user-reported reps-in-reserve for a strength exercise.
     * State-only; persistence happens via the existing saveWorkoutProgress /
     * completeWorkout pipeline which writes all exercises via
     * addExerciseToWorkout (Room @Insert with REPLACE). Pass null to clear a
     * previously-selected value.
     */
    fun setRir(exerciseId: String, rir: Int?) {
        require(rir == null || rir in 0..5) { "RIR must be null or 0..5" }
        val exercises = _uiState.value.exercises.map { exercise ->
            if (exercise.id == exerciseId) exercise.copy(rir = rir) else exercise
        }
        _uiState.value = _uiState.value.copy(exercises = exercises)
    }

    fun shuffleExercise(exerciseId: String) {
        viewModelScope.launch {
            val workout = currentWorkout ?: return@launch
            val currentExercise = _uiState.value.exercises.find { it.id == exerciseId } ?: return@launch
            val currentMuscleGroups = currentExercise.exercise.muscleGroups

            // Category pool: push/pull taxonomy (parity with GenerateWorkoutUseCase).
            // Prevents cross-side leaks regardless of muscle-group tagging.
            val categories = when (workout.type) {
                WorkoutType.PUSH -> listOf(ExerciseCategory.STRENGTH_PUSH)
                WorkoutType.PULL -> listOf(ExerciseCategory.STRENGTH_PULL, ExerciseCategory.STRENGTH_LEGS)
            }
            val categoryPool = exerciseRepository.getUserActiveExercisesByCategories(categories)

            // Equipment filter: only exercises performable at the current gym.
            val gym = workout.gymId?.let { gymRepository.getGymById(it) } ?: gymRepository.getDefaultGym()
            val equipmentCompatible = categoryPool.filter { exercise ->
                gym?.let { g -> EquipmentType.canPerformExercise(exercise.equipment, g.equipmentList) } ?: true
            }

            // Similarity: share >=1 muscle group with swapped exercise, and not already in workout.
            val currentExerciseIds = _uiState.value.exercises.map { it.exercise.id }
            val similar = equipmentCompatible.filter { exercise ->
                exercise.id !in currentExerciseIds &&
                    exercise.muscleGroups.any { it in currentMuscleGroups }
            }

            // Softened cooldown: prefer non-recent, fall back to full similar pool when
            // too few candidates. Keeps variety without forcing a hard 7-day wall.
            val recentExerciseIds = workoutRepository.getExerciseIdsFromLastWeek()
            val preferred = similar.filter { it.id !in recentExerciseIds }
            val poolBeforeMemory = if (preferred.size >= 5) preferred else similar

            // Shuffle memory: avoid repeating recent picks for this slot so variety
            // emerges across consecutive taps on the same ↻ icon. Fall back to the
            // full pool when memory would empty it — better to repeat than return nothing.
            val memory = shuffleMemory.getOrPut(exerciseId) { ArrayDeque() }
            val poolAfterMemory = poolBeforeMemory.filterNot { it.id in memory }
            val pool = if (poolAfterMemory.isNotEmpty()) poolAfterMemory else poolBeforeMemory

            if (com.workoutapp.BuildConfig.DEBUG) {
                android.util.Log.d("FortisLupus",
                    "shuffle-strength gym=${gym?.name} similar=${similar.size} " +
                        "afterCooldown=${preferred.size} poolBeforeMemory=${poolBeforeMemory.size} " +
                        "poolAfterMemory=${poolAfterMemory.size}")
            }

            if (pool.isNotEmpty()) {
                val picked = pool.random()
                memory.addLast(picked.id)
                while (memory.size > SHUFFLE_MEMORY_WINDOW) memory.removeFirst()
                replaceExercise(exerciseId, picked)
                // Rebuild prescriptions so the freshly-shuffled slot reflects the new
                // exercise's profile/history, not the stale prescription from the swap-out.
                val updatedPrescriptions = buildPrescriptions(_uiState.value.exercises)
                _uiState.value = _uiState.value.copy(prescriptions = updatedPrescriptions)
            }
        }
    }
    
    /**
     * Regenerate all exercises in the current workout. Reuses GenerateWorkoutUseCase
     * so every constraint (push/pull side, equipment, category, cooldown) matches
     * what the initial generator would produce. Keeps the same workout id/type/date
     * so in-progress restore, history, and block-periodization state are unaffected.
     */
    fun shuffleAllExercises() {
        viewModelScope.launch {
            val workout = currentWorkout ?: return@launch
            val generated = generateWorkoutUseCase(gymId = workout.gymId, typeOverride = workout.type)
            if (generated.exercises.isEmpty()) return@launch

            val newWorkoutExercises = generated.exercises.map { exercise ->
                WorkoutExercise(
                    id = UUID.randomUUID().toString(),
                    workoutId = workout.id,
                    exercise = exercise,
                    sets = listOf(com.workoutapp.domain.model.Set(reps = 0, weight = 0f, completed = false))
                )
            }
            val prescriptions = buildPrescriptions(newWorkoutExercises)
            _uiState.value = _uiState.value.copy(
                exercises = newWorkoutExercises,
                prescriptions = prescriptions
            )
            autosaveProgress()
        }
    }

    private fun replaceExercise(oldExerciseId: String, newExercise: Exercise) {
        // Preserve the slot UUID so shuffle memory (keyed by slot id) stays stable
        // across swaps and LazyColumn keys don't thrash. Clear prescription/rir since
        // those were computed for the exercise being swapped out.
        val exercises = _uiState.value.exercises.map { workoutExercise ->
            if (workoutExercise.id == oldExerciseId) {
                workoutExercise.copy(
                    exercise = newExercise,
                    sets = listOf(com.workoutapp.domain.model.Set(reps = 0, weight = 0f, completed = false)),
                    rir = null,
                    prescription = null,
                )
            } else {
                workoutExercise
            }
        }
        _uiState.value = _uiState.value.copy(exercises = exercises)
        autosaveProgress()
    }

    fun removeExercise(exerciseId: String) {
        val exercises = _uiState.value.exercises.filter { it.id != exerciseId }
        _uiState.value = _uiState.value.copy(exercises = exercises)
        autosaveProgress()
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
        autosaveProgress()
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
                    profile = profile,
                    weekInBlock = blockState?.weekInBlock ?: 1,
                    isDeloadWeek = blockState?.isDeloadWeek ?: false
                )
            } else {
                // Fallback: legacy history-based
                val targetId = workoutExercise.exercise.id
                val history = fullCandidates
                    .mapNotNull { w -> w.exercises.firstOrNull { it.exercise.id == targetId } }
                    .take(2)
                val legacy = StrengthSetPrescriber.prescribe(
                    positionInWorkout = index,
                    history = history,
                    equipment = workoutExercise.exercise.equipment,
                    progressionRate = profiles[workoutExercise.exercise.id]?.progressionRateLbPerMonth,
                    weekInBlock = blockState?.weekInBlock ?: 1,
                    isDeloadWeek = blockState?.isDeloadWeek ?: false
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
                deleteWorkoutUseCase(workout.id)
            }
            _uiState.value = _uiState.value.copy(isCompleted = true)
        }
    }
    
    fun saveWorkoutProgress() {
        viewModelScope.launch {
            currentWorkout?.let { workout ->
                // Sanitize RIR first (strip to null on incomplete exercises),
                // then transactional replace (clears stale rows from shuffled-out
                // exercises before inserting the sanitized current set).
                val exercisesToPersist = sanitizeRirForPersist(_uiState.value.exercises)
                workoutRepository.replaceExercisesForWorkout(workout.id, exercisesToPersist)

                val incompleteWorkout = workout.copy(
                    status = WorkoutStatus.INCOMPLETE,
                    exercises = exercisesToPersist
                )
                workoutRepository.updateWorkout(incompleteWorkout)

                _uiState.value = _uiState.value.copy(isCompleted = true)
            }
        }
    }

    /**
     * Silent persistence for lifecycle pauses (app backgrounded). Writes current
     * exercises + sets to Room but preserves WorkoutStatus.IN_PROGRESS so the
     * restore path at startWorkout() can recover this workout on relaunch.
     *
     * Diverges from saveWorkoutProgress() in two critical ways:
     *   - Does NOT flip status to INCOMPLETE (that would break IN_PROGRESS restore)
     *   - Does NOT set isCompleted = true (that would navigate away from screen)
     */
    fun autosaveProgress() {
        viewModelScope.launch {
            val workout = currentWorkout ?: return@launch
            // Sanitize first so stale RIR on incomplete exercises never lands
            // in Room, then transactional replace to clear shuffled-out rows.
            val exercisesToPersist = sanitizeRirForPersist(_uiState.value.exercises)
            workoutRepository.replaceExercisesForWorkout(workout.id, exercisesToPersist)
            workoutRepository.updateWorkout(workout.copy(exercises = exercisesToPersist))
        }
    }

    /**
     * "Effective now" Date that respects the debug date-offset preference, matching
     * the offset already applied to workout creation dates in startWorkout().
     */
    private fun effectiveNow(): Date {
        val prefs = getApplication<Application>().getSharedPreferences("debug_prefs", android.content.Context.MODE_PRIVATE)
        val dateOffset = prefs.getInt("date_offset", 0)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dateOffset)
        return cal.time
    }

    /**
     * RIR is only meaningful for a fully-completed exercise. Strip it otherwise
     * so the DB never holds a stale rir for an exercise with zero completed
     * volume (see round-2 validation notes).
     */
    private fun sanitizeRirForPersist(exercises: List<WorkoutExercise>): List<WorkoutExercise> =
        exercises.map { ex ->
            val allComplete = ex.sets.isNotEmpty() && ex.sets.all { it.completed }
            if (allComplete) ex else ex.copy(rir = null)
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
            Log.d("FortisLupus", "Fatigue overlap (strength): $warning")
        }
        return warning
    }

    fun completeWorkout() {
        viewModelScope.launch {
            currentWorkout?.let { workout ->
                // Sanitize RIR first, then transactional replace for atomicity.
                val exercisesToPersist = sanitizeRirForPersist(_uiState.value.exercises)
                workoutRepository.replaceExercisesForWorkout(workout.id, exercisesToPersist)

                // Compute session duration from start time
                val durationMin = ((System.currentTimeMillis() - workoutStartTime) / 60000).toInt()

                // Update workout status
                val completedWorkout = workout.copy(
                    status = WorkoutStatus.COMPLETED,
                    exercises = exercisesToPersist,
                    durationMinutes = durationMin
                )
                workoutRepository.updateWorkout(completedWorkout)

                // Update training profile
                profileComputerUseCase.updateAfterWorkout(completedWorkout.id)
                stravaSyncManager.queueWorkout(completedWorkout.id)

                // Block state persistence on completion
                gymId?.let { gId ->
                    val existing = blockStateRepository.getState(gId)
                    val absenceReason = com.workoutapp.domain.usecase.BlockPeriodization.DeloadReason.EXTENDED_ABSENCE
                    when {
                        // First-ever workout: initialize block 1 starting today
                        existing == null -> {
                            blockStateRepository.setState(gId, completedWorkout.date, 1)
                        }
                        // Returning from extended absence: reset with incremented block number
                        blockState?.deloadReason == absenceReason -> {
                            val newNumber = existing.second + 1
                            blockStateRepository.setState(gId, completedWorkout.date, newNumber)
                        }
                        // Completed scheduled or plateau week-4 deload: advance to next block
                        blockState?.weekInBlock == 4 -> {
                            blockStateRepository.advanceBlock(gId)
                        }
                    }
                }

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
    val isCompleted: Boolean = false,
    val fatigueWarning: String? = null
) {
    // True once the user has touched any set (checked off, typed reps, or typed weight).
    // Gates the back-nav cancel confirmation — don't interrupt a back-out when nothing
    // has been logged yet, since there's no progress to save or discard.
    val hasLoggedSets: Boolean
        get() = exercises.any { we ->
            we.sets.any { s -> s.completed || s.reps > 0 || s.weight > 0f }
        }
}

// Number of most-recent completed workouts to scan when building the
// per-exercise history window. Over-fetch beyond the 2-session minimum so
// sessions that didn't include a given exercise don't starve the lookup.
private const val HISTORY_LOOKBACK_WORKOUTS = 20

// Dropped from 3 → 2 on 2026-04-20. After equipment + muscle-overlap +
// 7-day cooldown filtering, the "similar" pool can be 5-6 exercises; a
// smaller memory window leaves more effective picks so repeated shuffle
// taps feel varied instead of cycling through the same 2-3 exercises.
private const val SHUFFLE_MEMORY_WINDOW = 2
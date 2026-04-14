package com.workoutapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutapp.domain.model.Gym
import com.workoutapp.domain.model.GymWorkoutStyle
import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutFormat
import com.workoutapp.domain.model.WorkoutStatus
import com.workoutapp.domain.model.WorkoutType
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.GymRepository
import com.workoutapp.domain.repository.UserPreferencesRepository
import com.workoutapp.domain.repository.WorkoutRepository
import com.workoutapp.domain.usecase.DeleteWorkoutUseCase
import com.workoutapp.domain.usecase.ExportWorkoutsUseCase
import com.workoutapp.domain.usecase.GenerateConditioningWorkoutUseCase
import com.workoutapp.domain.usecase.GenerateWorkoutUseCase
import com.workoutapp.domain.usecase.ImportDebugDataUseCase
import com.workoutapp.domain.usecase.ImportResult
import com.workoutapp.domain.usecase.ImportWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val gymRepository: GymRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val generateWorkoutUseCase: GenerateWorkoutUseCase,
    private val generateConditioningWorkoutUseCase: GenerateConditioningWorkoutUseCase,
    private val importWorkoutUseCase: ImportWorkoutUseCase,
    private val importDebugDataUseCase: ImportDebugDataUseCase,
    private val deleteWorkoutUseCase: DeleteWorkoutUseCase,
    private val exportWorkoutsUseCase: ExportWorkoutsUseCase,
    private val blockStateRepository: com.workoutapp.domain.repository.BlockStateRepository
) : ViewModel() {

    private val _lastWorkout = MutableStateFlow<Workout?>(null)
    val lastWorkout: StateFlow<Workout?> = _lastWorkout.asStateFlow()

    private val _recentWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val recentWorkouts: StateFlow<List<Workout>> = _recentWorkouts.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _gyms = MutableStateFlow<List<Gym>>(emptyList())
    val gyms: StateFlow<List<Gym>> = _gyms.asStateFlow()

    private val _selectedGymId = MutableStateFlow<Long?>(null)
    val selectedGymId: StateFlow<Long?> = _selectedGymId.asStateFlow()

    private val _nextWorkoutType = MutableStateFlow(WorkoutType.PULL)
    val nextWorkoutType: StateFlow<WorkoutType> = _nextWorkoutType.asStateFlow()

    private val _selectedGymStyle = MutableStateFlow<GymWorkoutStyle?>(null)
    val selectedGymStyle: StateFlow<GymWorkoutStyle?> = _selectedGymStyle.asStateFlow()

    private var recentWorkoutsJob: Job? = null

    // Predicted conditioning format for the Home Gym NextWorkoutCard. Null
    // when the selected gym is strength-only. Stable between recomputes so
    // the card doesn't flicker — updateNextType reseeds it on gym change /
    // workout completion, skip() flips it in place.
    private val _nextWorkoutFormat = MutableStateFlow<WorkoutFormat?>(null)
    val nextWorkoutFormat: StateFlow<WorkoutFormat?> = _nextWorkoutFormat.asStateFlow()

    private val _blockIndicator = MutableStateFlow<String?>(null)
    val blockIndicator: StateFlow<String?> = _blockIndicator.asStateFlow()

    private val _intensityHint = MutableStateFlow<String?>(null)
    val intensityHint: StateFlow<String?> = _intensityHint.asStateFlow()

    init {
        loadLastWorkout()
        loadGyms()
    }

    private fun loadGyms() {
        viewModelScope.launch {
            val allGyms = gymRepository.getAllGyms()
            _gyms.value = allGyms
            // Initial selection: stored preference, else default gym, else first gym.
            val storedId = userPreferencesRepository.selectedGymId().firstOrNull()
            val resolvedId = when {
                storedId != null && allGyms.any { it.id == storedId } -> storedId
                else -> allGyms.firstOrNull { it.isDefault }?.id ?: allGyms.firstOrNull()?.id
            }
            _selectedGymId.value = resolvedId
            updateNextType(resolvedId)
            loadRecentWorkouts(resolvedId)
        }
    }

    fun selectGym(gymId: Long) {
        _selectedGymId.value = gymId
        viewModelScope.launch {
            userPreferencesRepository.setSelectedGymId(gymId)
        }
        updateNextType(gymId)
        loadRecentWorkouts(gymId)
    }
    
    private fun loadLastWorkout() {
        viewModelScope.launch {
            _lastWorkout.value = workoutRepository.getLastWorkout()
        }
    }
    
    private fun loadRecentWorkouts(gymId: Long?) {
        recentWorkoutsJob?.cancel()
        if (gymId == null) {
            _recentWorkouts.value = emptyList()
            return
        }
        recentWorkoutsJob = viewModelScope.launch {
            workoutRepository.getCompletedWorkoutsByGym(gymId).collect { workouts ->
                // Load full workout details for each workout
                val detailedWorkouts = workouts.take(50).map { workout ->
                    workoutRepository.getWorkoutById(workout.id) ?: workout
                }
                _recentWorkouts.value = detailedWorkouts
                // Recompute next workout type whenever the completed-workouts stream
                // updates (i.e. after the user finishes a workout and comes back).
                updateNextType(_selectedGymId.value)
            }
        }
    }

    private fun updateNextType(gymId: Long?) {
        if (gymId == null) return
        // Update the style first so HomeScreen can branch the card even before
        // the predict call resolves.
        val style = _gyms.value.firstOrNull { it.id == gymId }?.workoutStyle
        _selectedGymStyle.value = style
        if (style == GymWorkoutStyle.CONDITIONING) {
            viewModelScope.launch {
                _nextWorkoutFormat.value =
                    generateConditioningWorkoutUseCase.predictNextFormat(gymId)
                recomputeIntensityHint(gymId)
            }
            _blockIndicator.value = null
            return
        }
        // Strength gym — clear any stale format from a prior CONDITIONING gym.
        _nextWorkoutFormat.value = null
        viewModelScope.launch {
            _nextWorkoutType.value = generateWorkoutUseCase.predictNextType(gymId)
            // Null state = no completed workouts yet; show synthetic Week 1 without persisting
            val persisted = blockStateRepository.getState(gymId)
            val (blockStart, blockNumber) = persisted ?: (java.util.Date() to 1)
            val lastWorkout = workoutRepository.getLastCompletedWorkoutByGym(gymId)
            val state = com.workoutapp.domain.usecase.BlockPeriodization.computeState(
                blockStartDate = blockStart,
                blockNumber = blockNumber,
                lastWorkoutDate = lastWorkout?.date,
                plateauedExerciseCount = 0
            )
            _blockIndicator.value = state.phaseLabel
            recomputeIntensityHint(gymId)
        }
    }

    private fun effectiveNow(): java.util.Date {
        val prefs = context.getSharedPreferences("debug_prefs", android.content.Context.MODE_PRIVATE)
        val dateOffset = prefs.getInt("date_offset", 0)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, dateOffset)
        return cal.time
    }

    /**
     * Recomputes the intensity-stacking hint using the currently previewed workout
     * type/format for [gymId]. Must be called any time the preview changes
     * (updateNextType, skip, gym switch, workout completion).
     */
    private fun recomputeIntensityHint(gymId: Long?) {
        if (gymId == null) {
            _intensityHint.value = null
            return
        }
        viewModelScope.launch {
            try {
                val now = effectiveNow()
                val since = java.util.Date(now.time - java.util.concurrent.TimeUnit.DAYS.toMillis(
                    com.workoutapp.domain.usecase.FatigueAwareness.INTENSITY_LOOKBACK_DAYS
                ))
                val recent = workoutRepository.getCompletedWorkoutSummariesSince(since)

                val style = _selectedGymStyle.value
                val plannedFormat: WorkoutFormat
                val plannedDuration: Int?
                when (style) {
                    GymWorkoutStyle.STRENGTH -> {
                        plannedFormat = WorkoutFormat.STRENGTH
                        plannedDuration = null
                    }
                    GymWorkoutStyle.CONDITIONING -> {
                        plannedFormat = _nextWorkoutFormat.value ?: WorkoutFormat.AMRAP
                        // Match canonical durations used by GenerateConditioningWorkoutUseCase.
                        plannedDuration = when (plannedFormat) {
                            WorkoutFormat.EMOM -> 20
                            WorkoutFormat.AMRAP -> 15
                            else -> null
                        }
                    }
                    null -> {
                        _intensityHint.value = null
                        return@launch
                    }
                }
                val plannedIntensity = com.workoutapp.domain.usecase.FatigueAwareness.classify(
                    plannedFormat, plannedDuration
                )
                val hint = com.workoutapp.domain.usecase.FatigueAwareness.checkIntensityStacking(
                    recentCompleted = recent,
                    plannedIntensity = plannedIntensity
                )
                if (hint != null) {
                    android.util.Log.d("FortisLupus", "Fatigue intensity hint: $hint")
                }
                _intensityHint.value = hint
            } catch (e: Exception) {
                android.util.Log.w("FortisLupus", "Intensity hint computation failed", e)
                _intensityHint.value = null
            }
        }
    }

    /**
     * Flips the previewed workout for the current gym. On strength gyms this
     * toggles PUSH ↔ PULL; on conditioning gyms it toggles EMOM ↔ AMRAP. The
     * flip is session-local — the next updateNextType (after gym switch or
     * workout completion) recomputes from scratch.
     */
    fun skip() {
        when (_selectedGymStyle.value) {
            GymWorkoutStyle.CONDITIONING -> {
                _nextWorkoutFormat.value = when (_nextWorkoutFormat.value) {
                    WorkoutFormat.AMRAP -> WorkoutFormat.EMOM
                    else -> WorkoutFormat.AMRAP
                }
            }
            GymWorkoutStyle.STRENGTH -> {
                _nextWorkoutType.value = when (_nextWorkoutType.value) {
                    WorkoutType.PUSH -> WorkoutType.PULL
                    WorkoutType.PULL -> WorkoutType.PUSH
                }
            }
            null -> Unit
        }
        recomputeIntensityHint(_selectedGymId.value)
    }

    fun calculateTotalWeight(workout: Workout): Float {
        return workout.exercises.sumOf { exercise ->
            exercise.sets.sumOf { set ->
                if (set.completed) (set.weight * set.reps).toDouble() else 0.0
            }
        }.toFloat()
    }
    
    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            deleteWorkoutUseCase(workoutId)
        }
    }

    fun importWorkouts(csvContent: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            
            try {
                val result = importWorkoutUseCase.importFromCsvContent(csvContent)
                _importState.value = ImportState.Success(result)
                
                // Reload workouts after import
                loadLastWorkout()
                loadRecentWorkouts(_selectedGymId.value)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    fun resetImportState() {
        _importState.value = ImportState.Idle
    }
    
    private val _exportCsv = MutableStateFlow<String?>(null)
    val exportCsv: StateFlow<String?> = _exportCsv.asStateFlow()

    fun exportWorkouts() {
        viewModelScope.launch {
            _exportCsv.value = exportWorkoutsUseCase()
        }
    }

    fun clearExport() {
        _exportCsv.value = null
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
            loadRecentWorkouts(_selectedGymId.value)
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
                loadRecentWorkouts(_selectedGymId.value)
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
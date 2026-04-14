# Block Periodization + Deload Automation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 4-week block periodization with scheduled + plateau + extended-absence deload triggers for LMU strength workouts.

**Architecture:** A `BlockPeriodization` pure utility computes week-in-block state. A `BlockStateRepository` persists block start date + number per-gym in SharedPreferences (no DB migration). `StrengthSetPrescriber` gains `weekInBlock` and `isDeloadWeek` params that modify sets/reps/weight. `GenerateWorkoutUseCase` computes block state and passes it through. `HomeViewModel` exposes a block indicator string for the NextWorkoutCard.

**Tech Stack:** Kotlin, Hilt DI, SharedPreferences, existing MVVM

**Non-Goals:** Periodization for conditioning (Phase 1 handled it). No DB changes. No user-configurable block length.

**Rollback:** Block state lives in SharedPreferences. Reverting code restores Phase 1 behavior. Orphaned pref keys are harmless.

---

## File Map

| File | Action | Responsibility |
|------|--------|---------------|
| `domain/usecase/BlockPeriodization.kt` | Create | Pure state computation (weekInBlock, deloadReason, phaseLabel) |
| `domain/repository/BlockStateRepository.kt` | Create | Interface for block state storage |
| `data/repository/BlockStateRepositoryImpl.kt` | Create | SharedPreferences-backed implementation |
| `di/RepositoryModule.kt` | Modify | Hilt binding for BlockStateRepository |
| `domain/usecase/StrengthSetPrescriber.kt` | Modify | weekInBlock + isDeloadWeek progression logic |
| `domain/usecase/GenerateWorkoutUseCase.kt` | Modify | Compute block state, advance block on week 4 |
| `domain/model/BlockState.kt` | Create | Data class returned by GenerateWorkoutUseCase |
| `domain/model/GeneratedWorkout.kt` | Modify | Add blockState field |
| `presentation/viewmodel/WorkoutViewModel.kt` | Modify | Pass block state to prescriber, advance block on completion |
| `presentation/viewmodel/HomeViewModel.kt` | Modify | Expose blockIndicator string for NextWorkoutCard |
| `presentation/ui/home/HomeScreen.kt` | Modify | Render block indicator caption |

---

### Task 1: Create BlockPeriodization utility

**Files:**
- Create: `app/src/main/java/com/workoutapp/domain/usecase/BlockPeriodization.kt`

- [ ] **Step 1: Create the utility**

Write the following to the file:

```kotlin
package com.workoutapp.domain.usecase

import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Computes 4-week block periodization state for LMU strength workouts.
 *
 * Block cycle:
 *   Week 1: Volume phase · Baseline
 *   Week 2: Volume phase · Add reps (targetRepsMax + 1)
 *   Week 3: Intensity phase · Add weight (+5 lb if cleared reps last session)
 *   Week 4: Deload · Recovery (weight × 0.80, sets - 1)
 *
 * Deload triggers:
 *   - SCHEDULED: weekInBlock == 4 via calendar math
 *   - PLATEAU: 2+ exercises in upcoming workout have plateauFlag == true
 *   - EXTENDED_ABSENCE: last workout was 14+ days ago → reset to week 1 (labeled as deload)
 */
object BlockPeriodization {

    const val BLOCK_WEEKS = 4
    const val EXTENDED_ABSENCE_DAYS = 14
    const val PLATEAU_TRIGGER_COUNT = 2
    const val DELOAD_WEIGHT_PCT = 0.80f

    enum class DeloadReason { SCHEDULED, PLATEAU, EXTENDED_ABSENCE }

    data class State(
        val blockNumber: Int,
        val weekInBlock: Int,            // 1, 2, 3, or 4
        val isDeloadWeek: Boolean,
        val deloadReason: DeloadReason?,
        val phaseLabel: String           // Plain-language description for UI
    )

    fun computeState(
        blockStartDate: Date,
        blockNumber: Int,
        lastWorkoutDate: Date?,
        plateauedExerciseCount: Int,
        now: Date = Date()
    ): State {
        // Extended absence override: returning from 14+ day gap starts a new block at week 1
        if (lastWorkoutDate != null) {
            val daysSinceLast = daysBetween(lastWorkoutDate, now)
            if (daysSinceLast >= EXTENDED_ABSENCE_DAYS) {
                return State(
                    blockNumber = blockNumber,
                    weekInBlock = 1,
                    isDeloadWeek = true,
                    deloadReason = DeloadReason.EXTENDED_ABSENCE,
                    phaseLabel = "Week 1 · Returning to training"
                )
            }
        }

        // Calendar-based week calculation
        val daysSinceStart = daysBetween(blockStartDate, now).coerceAtLeast(0)
        val weeksSinceStart = (daysSinceStart / 7).toInt()
        val rawWeek = (weeksSinceStart % BLOCK_WEEKS) + 1

        // Scheduled deload on week 4
        if (rawWeek == BLOCK_WEEKS) {
            return State(
                blockNumber = blockNumber,
                weekInBlock = 4,
                isDeloadWeek = true,
                deloadReason = DeloadReason.SCHEDULED,
                phaseLabel = "Deload week · Recovery"
            )
        }

        // Plateau override: force deload if 2+ exercises plateaued
        if (plateauedExerciseCount >= PLATEAU_TRIGGER_COUNT) {
            return State(
                blockNumber = blockNumber,
                weekInBlock = 4,
                isDeloadWeek = true,
                deloadReason = DeloadReason.PLATEAU,
                phaseLabel = "Deload week · Plateau recovery"
            )
        }

        // Normal weeks
        val label = when (rawWeek) {
            1 -> "Week 1 of 4 · Volume phase · Baseline"
            2 -> "Week 2 of 4 · Volume phase · Add reps"
            3 -> "Week 3 of 4 · Intensity phase · Add weight"
            else -> "Week $rawWeek of 4"
        }
        return State(
            blockNumber = blockNumber,
            weekInBlock = rawWeek,
            isDeloadWeek = false,
            deloadReason = null,
            phaseLabel = label
        )
    }

    /**
     * Returns the Monday 00:00 local time after the given date.
     * If the given date is already a Monday, returns the next Monday (+7 days).
     */
    fun nextMondayAfter(date: Date): Date {
        val cal = Calendar.getInstance().apply { time = date }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val daysFromMonday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        val advance = if (daysFromMonday == 0) 7 else 7 - daysFromMonday
        cal.add(Calendar.DAY_OF_YEAR, advance)
        return cal.time
    }

    private fun daysBetween(start: Date, end: Date): Long {
        val diffMillis = end.time - start.time
        return TimeUnit.MILLISECONDS.toDays(diffMillis)
    }
}
```

- [ ] **Step 2: Compile**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/workoutapp/domain/usecase/BlockPeriodization.kt
git commit -m "feat: Add BlockPeriodization utility for 4-week block state"
```

---

### Task 2: Create BlockStateRepository

**Files:**
- Create: `app/src/main/java/com/workoutapp/domain/repository/BlockStateRepository.kt`
- Create: `app/src/main/java/com/workoutapp/data/repository/BlockStateRepositoryImpl.kt`
- Modify: `app/src/main/java/com/workoutapp/di/RepositoryModule.kt`

- [ ] **Step 1: Create the interface**

Write to `app/src/main/java/com/workoutapp/domain/repository/BlockStateRepository.kt`:

```kotlin
package com.workoutapp.domain.repository

import java.util.Date

/**
 * Persists 4-week block periodization state per gym in SharedPreferences.
 * No DB schema changes — pure runtime state, orphaned keys harmless on rollback.
 */
interface BlockStateRepository {
    /**
     * Returns (blockStartDate, blockNumber) for this gym.
     * If no state exists, initializes to (today, 1) and returns it.
     */
    suspend fun getState(gymId: Long): Pair<Date, Int>

    /**
     * Advances the block: sets start date to the next Monday after today,
     * increments block number by 1. Called after completing a deload workout.
     */
    suspend fun advanceBlock(gymId: Long)
}
```

- [ ] **Step 2: Create the implementation**

Write to `app/src/main/java/com/workoutapp/data/repository/BlockStateRepositoryImpl.kt`:

```kotlin
package com.workoutapp.data.repository

import android.content.Context
import com.workoutapp.domain.repository.BlockStateRepository
import com.workoutapp.domain.usecase.BlockPeriodization
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockStateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BlockStateRepository {

    private val prefs by lazy {
        context.getSharedPreferences("block_state_prefs", Context.MODE_PRIVATE)
    }

    override suspend fun getState(gymId: Long): Pair<Date, Int> {
        val startKey = "block_start_date_$gymId"
        val numberKey = "block_number_$gymId"

        val storedStart = prefs.getLong(startKey, 0L)
        return if (storedStart == 0L) {
            // First-time: initialize to today as block 1
            val today = Date()
            prefs.edit()
                .putLong(startKey, today.time)
                .putInt(numberKey, 1)
                .apply()
            today to 1
        } else {
            Date(storedStart) to prefs.getInt(numberKey, 1)
        }
    }

    override suspend fun advanceBlock(gymId: Long) {
        val startKey = "block_start_date_$gymId"
        val numberKey = "block_number_$gymId"
        val newStart = BlockPeriodization.nextMondayAfter(Date())
        val currentNumber = prefs.getInt(numberKey, 1)
        prefs.edit()
            .putLong(startKey, newStart.time)
            .putInt(numberKey, currentNumber + 1)
            .apply()
    }
}
```

- [ ] **Step 3: Add Hilt binding**

Open `app/src/main/java/com/workoutapp/di/RepositoryModule.kt`. Inside the abstract class, add:

```kotlin
@Binds
@Singleton
abstract fun bindBlockStateRepository(
    blockStateRepositoryImpl: com.workoutapp.data.repository.BlockStateRepositoryImpl
): com.workoutapp.domain.repository.BlockStateRepository
```

(Match the existing binding style — most bindings use fully-qualified names or imports. Use whichever matches the file.)

- [ ] **Step 4: Compile**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/workoutapp/domain/repository/BlockStateRepository.kt \
       app/src/main/java/com/workoutapp/data/repository/BlockStateRepositoryImpl.kt \
       app/src/main/java/com/workoutapp/di/RepositoryModule.kt
git commit -m "feat: Add BlockStateRepository for per-gym periodization state"
```

---

### Task 3: Add block state to GeneratedWorkout and wire through GenerateWorkoutUseCase

**Files:**
- Modify: `app/src/main/java/com/workoutapp/domain/model/GeneratedWorkout.kt`
- Modify: `app/src/main/java/com/workoutapp/domain/usecase/GenerateWorkoutUseCase.kt`

- [ ] **Step 1: Add blockState to GeneratedWorkout**

Open `app/src/main/java/com/workoutapp/domain/model/GeneratedWorkout.kt`. The current data class looks like:

```kotlin
data class GeneratedWorkout(
    val type: WorkoutType,
    val exercises: List<Exercise>
)
```

Add an optional `blockState` field:

```kotlin
data class GeneratedWorkout(
    val type: WorkoutType,
    val exercises: List<Exercise>,
    val blockState: com.workoutapp.domain.usecase.BlockPeriodization.State? = null
)
```

- [ ] **Step 2: Inject BlockStateRepository into GenerateWorkoutUseCase**

Open `app/src/main/java/com/workoutapp/domain/usecase/GenerateWorkoutUseCase.kt`. Add to the constructor:

```kotlin
class GenerateWorkoutUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val gymRepository: GymRepository,
    private val profileRepository: TrainingProfileRepository,
    private val blockStateRepository: com.workoutapp.domain.repository.BlockStateRepository
) {
```

- [ ] **Step 3: Compute block state and include it in the return value**

At the bottom of `invoke()`, replace the existing return statement:

```kotlin
return GeneratedWorkout(type = workoutType, exercises = selected)
```

With:

```kotlin
// Block periodization (LMU strength only — gymId required)
val blockState = if (gymId != null) {
    val (blockStart, blockNumber) = blockStateRepository.getState(gymId)
    val lastWorkout = workoutRepository.getLastCompletedWorkoutByGym(gymId)
    val plateauedCount = selected.count { ex ->
        exerciseProfiles[ex.id]?.plateauFlag == true
    }
    BlockPeriodization.computeState(
        blockStartDate = blockStart,
        blockNumber = blockNumber,
        lastWorkoutDate = lastWorkout?.date,
        plateauedExerciseCount = plateauedCount
    ).also { state ->
        Log.d(TAG, "Block ${state.blockNumber} ${state.phaseLabel} (plateauCount=$plateauedCount)")
    }
} else null

return GeneratedWorkout(type = workoutType, exercises = selected, blockState = blockState)
```

Add the import at the top of the file:

```kotlin
import com.workoutapp.domain.usecase.BlockPeriodization
```

(Since it's in the same package, the import may not be strictly necessary, but add it for clarity.)

- [ ] **Step 4: Compile**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/workoutapp/domain/model/GeneratedWorkout.kt \
       app/src/main/java/com/workoutapp/domain/usecase/GenerateWorkoutUseCase.kt
git commit -m "feat: Compute block state in GenerateWorkoutUseCase"
```

---

### Task 4: Weekly progression in StrengthSetPrescriber

**Files:**
- Modify: `app/src/main/java/com/workoutapp/domain/usecase/StrengthSetPrescriber.kt`

- [ ] **Step 1: Add weekInBlock and isDeloadWeek parameters**

Change the `prescribe()` signature:

```kotlin
fun prescribe(
    positionInWorkout: Int,
    history: List<WorkoutExercise>,
    equipment: String = "",
    progressionRate: Float? = null,
    weekInBlock: Int = 1,
    isDeloadWeek: Boolean = false
): StrengthPrescription {
```

- [ ] **Step 2: Apply block progression to positions 0 and 1**

Inside `prescribe()`, find the block where the successful progression path returns (`mostRecentClearedTop && previousClearedTop && mostRecentWorkingWeight == previousWorkingWeight`). Wrap the existing behavior with block-aware modifications.

Add this helper method to the object (near the bottom, alongside `isDumbbellSlowProgressor`):

```kotlin
/**
 * Applies block-weekly progression on top of base prescription.
 * Only affects positions 0 (anchor) and 1 (hypertrophy). Accessory (position 2+)
 * uses base Phase 1 logic unchanged.
 */
private fun applyBlockProgression(
    base: StrengthPrescription,
    positionInWorkout: Int,
    weekInBlock: Int,
    isDeloadWeek: Boolean
): StrengthPrescription {
    if (positionInWorkout >= 2) return base  // accessory unaffected

    if (isDeloadWeek) {
        val deloadWeight = base.recommendedWeight?.let {
            roundToNearest5(it * BlockPeriodization.DELOAD_WEIGHT_PCT)
        }
        val deloadSets = (base.targetSets - 1).coerceAtLeast(2)
        return base.copy(
            targetSets = deloadSets,
            recommendedWeight = deloadWeight,
            rationale = "Deload · ${deloadWeight?.toInt() ?: "bodyweight"} lb, ${deloadSets} sets — recovery week"
        )
    }

    return when (weekInBlock) {
        1 -> base // baseline — unchanged
        2 -> {
            // Add reps: bump targetRepsMax + 1, cap at +1, skip if max ≤ 5
            if (base.targetRepsMax <= 5) base
            else base.copy(
                targetRepsMax = base.targetRepsMax + 1,
                rationale = "Week 2 · aim for ${base.targetRepsMax + 1} reps (same weight) to earn +5 lb next week"
            )
        }
        3 -> base  // Week 3 "add weight" is already handled by Phase 1's +5 lb rule
        else -> base
    }
}
```

- [ ] **Step 3: Apply the helper in the prescribe flow**

The prescribe() method has two return paths: the "cleared reps" path and the fallback paths. Apply block progression to the prescription BEFORE returning in each relevant spot.

Find the successful progression return block:

```kotlin
return if (
    mostRecentClearedTop &&
    previousClearedTop &&
    mostRecentWorkingWeight == previousWorkingWeight
) {
    if (isDumbbellSlowProgressor(equipment, progressionRate)) {
        StrengthPrescription(...)
    } else {
        StrengthPrescription(...)
    }
} else {
    StrengthPrescription(...)  // repeat weight path
}
```

Wrap the entire `return` expression in `applyBlockProgression(...)`:

```kotlin
val basePrescription = if (
    mostRecentClearedTop &&
    previousClearedTop &&
    mostRecentWorkingWeight == previousWorkingWeight
) {
    if (isDumbbellSlowProgressor(equipment, progressionRate)) {
        StrengthPrescription(
            targetSets = template.sets,
            targetRepsMin = template.repsMin,
            targetRepsMax = template.repsMax + 1,
            recommendedWeight = mostRecentWorkingWeight,
            rationale = "hold ${mostRecentWorkingWeight.toInt()} lb — aim for ${template.repsMax + 1} reps to earn +${PROGRESSION_DELTA_LB.toInt()} lb"
        )
    } else {
        StrengthPrescription(
            targetSets = template.sets,
            targetRepsMin = template.repsMin,
            targetRepsMax = template.repsMax,
            recommendedWeight = mostRecentWorkingWeight + PROGRESSION_DELTA_LB,
            rationale = "+${PROGRESSION_DELTA_LB.toInt()} lb — cleared ${template.repsMax} reps last 2 sessions"
        )
    }
} else {
    StrengthPrescription(
        targetSets = template.sets,
        targetRepsMin = template.repsMin,
        targetRepsMax = template.repsMax,
        recommendedWeight = mostRecentWorkingWeight,
        rationale = "repeat weight — aim for ${template.repsMax} reps to unlock progression"
    )
}

return applyBlockProgression(basePrescription, positionInWorkout, weekInBlock, isDeloadWeek)
```

Also apply to the "first time / history.size < 2" return at the top of prescribe():

```kotlin
if (history.size < 2) {
    Log.d(TAG, "Prescribe pos=$positionInWorkout: history fallback, sessions=${history.size}")
    val base = StrengthPrescription(
        targetSets = template.sets,
        targetRepsMin = template.repsMin,
        targetRepsMax = template.repsMax,
        recommendedWeight = null,
        rationale = if (history.isEmpty()) {
            "first time — find your working weight"
        } else {
            "returning — match last session and build history"
        }
    )
    return applyBlockProgression(base, positionInWorkout, weekInBlock, isDeloadWeek)
}
```

And the "incomplete history" return:

```kotlin
if (lastTwo.any { it.isEmpty() }) {
    val base = StrengthPrescription(
        targetSets = template.sets,
        targetRepsMin = template.repsMin,
        targetRepsMax = template.repsMax,
        recommendedWeight = null,
        rationale = "incomplete history — hit target reps to unlock progression"
    )
    return applyBlockProgression(base, positionInWorkout, weekInBlock, isDeloadWeek)
}
```

- [ ] **Step 4: Compile**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/workoutapp/domain/usecase/StrengthSetPrescriber.kt
git commit -m "feat: Apply block periodization progression in StrengthSetPrescriber"
```

---

### Task 5: Pass block state through WorkoutViewModel

**Files:**
- Modify: `app/src/main/java/com/workoutapp/presentation/viewmodel/WorkoutViewModel.kt`

- [ ] **Step 1: Store block state from generated workout**

In `WorkoutViewModel`, after the `currentWorkout` field, add:

```kotlin
private var blockState: BlockPeriodization.State? = null
```

Add the import at the top:

```kotlin
import com.workoutapp.domain.usecase.BlockPeriodization
```

- [ ] **Step 2: Capture block state from GeneratedWorkout**

In `startWorkout()`, after the line `val generated = generateWorkoutUseCase(gymId, typeOverride)`, add:

```kotlin
blockState = generated.blockState
```

- [ ] **Step 3: Pass weekInBlock and isDeloadWeek to prescriber calls**

Find `buildPrescriptions()` method. The existing call to `StrengthSetPrescriber.prescribe(...)` needs new params. Update both call sites:

```kotlin
val legacy = StrengthSetPrescriber.prescribe(
    positionInWorkout = index,
    history = history,
    equipment = workoutExercise.exercise.equipment,
    progressionRate = profiles[workoutExercise.exercise.id]?.progressionRateLbPerMonth,
    weekInBlock = blockState?.weekInBlock ?: 1,
    isDeloadWeek = blockState?.isDeloadWeek ?: false
)
```

- [ ] **Step 4: Advance block on week 4 completion**

In `completeWorkout()`, after the workout is marked complete (after `workoutRepository.updateWorkout(completedWorkout)` but before navigation), add:

```kotlin
// Advance block if we just completed the deload week (week 4)
if (blockState?.weekInBlock == 4 && blockState?.deloadReason != BlockPeriodization.DeloadReason.EXTENDED_ABSENCE) {
    gymId?.let { gId -> blockStateRepository.advanceBlock(gId) }
}
```

Inject `BlockStateRepository` into the constructor:

```kotlin
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    // ... existing params ...
    private val blockStateRepository: com.workoutapp.domain.repository.BlockStateRepository
) : AndroidViewModel(application) {
```

(The EXTENDED_ABSENCE case does NOT advance the block because it was already reset to week 1 by the absence detection.)

- [ ] **Step 5: Compile**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/workoutapp/presentation/viewmodel/WorkoutViewModel.kt
git commit -m "feat: Pass block state through WorkoutViewModel, advance on week 4 completion"
```

---

### Task 6: Block indicator on home screen

**Files:**
- Modify: `app/src/main/java/com/workoutapp/presentation/viewmodel/HomeViewModel.kt`
- Modify: `app/src/main/java/com/workoutapp/presentation/ui/home/HomeScreen.kt`

- [ ] **Step 1: Expose block indicator from HomeViewModel**

Inject `BlockStateRepository` into HomeViewModel constructor:

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    // ... existing params ...
    private val blockStateRepository: com.workoutapp.domain.repository.BlockStateRepository
) : ViewModel() {
```

Add a new StateFlow property:

```kotlin
private val _blockIndicator = MutableStateFlow<String?>(null)
val blockIndicator: StateFlow<String?> = _blockIndicator.asStateFlow()
```

Add the import:

```kotlin
import com.workoutapp.domain.usecase.BlockPeriodization
```

- [ ] **Step 2: Compute block indicator on gym selection**

In `updateNextType()`, after the existing logic, add block indicator computation for strength gyms only:

```kotlin
private fun updateNextType(gymId: Long?) {
    if (gymId == null) return
    val style = _gyms.value.firstOrNull { it.id == gymId }?.workoutStyle
    _selectedGymStyle.value = style
    if (style == GymWorkoutStyle.CONDITIONING) {
        viewModelScope.launch {
            _nextWorkoutFormat.value =
                generateConditioningWorkoutUseCase.predictNextFormat(gymId)
        }
        _blockIndicator.value = null  // no periodization for conditioning
        return
    }
    _nextWorkoutFormat.value = null
    viewModelScope.launch {
        _nextWorkoutType.value = generateWorkoutUseCase.predictNextType(gymId)
        val (blockStart, blockNumber) = blockStateRepository.getState(gymId)
        val lastWorkout = workoutRepository.getLastCompletedWorkoutByGym(gymId)
        val state = BlockPeriodization.computeState(
            blockStartDate = blockStart,
            blockNumber = blockNumber,
            lastWorkoutDate = lastWorkout?.date,
            plateauedExerciseCount = 0  // conservative estimate — final state computed at generation time
        )
        _blockIndicator.value = state.phaseLabel
    }
}
```

Note: `workoutRepository` is already injected — use the existing reference.

- [ ] **Step 3: Render the indicator in NextWorkoutCard**

Open `app/src/main/java/com/workoutapp/presentation/ui/home/HomeScreen.kt`. Find the `NextWorkoutCard` composable. Locate the subtitle content section (renders "Pack Run" / "Alpha Training" / "Pack Strength").

Add a new parameter to the `NextWorkoutCard` composable:

```kotlin
@Composable
fun NextWorkoutCard(
    nextWorkoutType: WorkoutType,
    nextWorkoutFormat: WorkoutFormat?,
    gymStyle: GymWorkoutStyle?,
    dateOffset: Int,
    blockIndicator: String? = null,  // NEW
    onTap: () -> Unit,
    onSkip: () -> Unit
) {
```

After the subtitle block (the "Alpha Training" / "Pack Strength" text with duration/muscle groups, before the "Tap to begin" row), add:

```kotlin
if (blockIndicator != null) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = blockIndicator,
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.7f)
    )
}
```

- [ ] **Step 4: Wire up the indicator from HomeScreen**

In the HomeScreen composable, collect the new state and pass to NextWorkoutCard:

```kotlin
val blockIndicator by viewModel.blockIndicator.collectAsState()

// ...

NextWorkoutCard(
    nextWorkoutType = nextWorkoutType,
    nextWorkoutFormat = nextWorkoutFormat,
    gymStyle = selectedGymStyle,
    dateOffset = testDateOffset,
    blockIndicator = blockIndicator,
    onTap = { /* existing */ },
    onSkip = { viewModel.skip() }
)
```

- [ ] **Step 5: Compile**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/workoutapp/presentation/viewmodel/HomeViewModel.kt \
       app/src/main/java/com/workoutapp/presentation/ui/home/HomeScreen.kt
git commit -m "feat: Show block indicator on home screen NextWorkoutCard"
```

---

### Task 7: Full build and verify

- [ ] **Step 1: Full build**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify with logcat**

After installing, check:

```bash
adb logcat -s FortisLupus
```

Expected output should include:
- "Block 1 Week 1 of 4 · Volume phase · Baseline (plateauCount=0)"
- On deload week: "Block N Deload week · Recovery (plateauCount=...)"
- If plateau: "Block N Deload week · Plateau recovery"
- If extended absence: "Block N Week 1 · Returning to training"

- [ ] **Step 3: Manual verification on device**

1. Select LMU Gym — verify block indicator shows "Week 1 of 4 · Volume phase · Baseline"
2. Select Home Gym — verify block indicator is hidden (no periodization for conditioning)
3. Complete a workout — verify prescription reflects current week (rationale mentions week 2 / deload etc.)
4. On week 4 completion — verify next generated workout shows "Week 1 of 4" of a new block number

No INIT_VERSION bump needed — this change is pure generation logic with no seeding changes.

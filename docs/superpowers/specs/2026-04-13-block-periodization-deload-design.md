# Block Periodization + Deload Automation — Phase 2

## Context

Phase 1 (shipped) gave smart recommendations: format alternation, exercise freshness decay, bench priority, equipment-aware progression. But workouts are still individually generated — no structured cycling of intensity over time. A real program uses periodization: progressive overload through a cycle, then a deload to recover.

Phase 2 adds a 4-week block periodization model for LMU strength workouts. Home Gym conditioning stays format-alternation only (already handled in Phase 1).

## Goal

Implement a calendar-based 4-week block cycle for LMU strength: weeks 1-3 progressive overload (volume-first), week 4 auto-deload. Plateau detection on 2+ exercises forces early deload. Extended absence (14+ days) resets to week 1.

## Non-Goals

- Periodization for conditioning (EMOM/AMRAP handled in Phase 1)
- RPE/RIR tracking (Phase 4)
- User-configurable block length (4 weeks is standard, keep it fixed)
- Preference learning / autoregulation beyond plateau detection (Phase 4)
- A dedicated periodization screen (home screen indicator only)

## Rollback Path

Block state lives in SharedPreferences (`workout_app_prefs`), not database. No schema changes, no migration. Reverting the code restores Phase 1 behavior. The orphaned preference keys are harmless.

## Changes

### 1. Block State in SharedPreferences

Two new preference keys per gym, so LMU Gym and any future strength gym track independently:

```
block_start_date_{gymId}: Long   // epoch millis, start of current block
block_number_{gymId}: Int        // 1, 2, 3, ... for display
```

Initial state (no prefs yet): `block_start_date = today`, `block_number = 1`.

### 2. Block State Calculator

New file: `app/src/main/java/com/workoutapp/domain/usecase/BlockPeriodization.kt`

```kotlin
object BlockPeriodization {
    const val BLOCK_WEEKS = 4
    const val EXTENDED_ABSENCE_DAYS = 14
    const val DELOAD_WEIGHT_PCT = 0.80f

    data class BlockState(
        val blockNumber: Int,
        val weekInBlock: Int,  // 1, 2, 3, or 4
        val isDeloadWeek: Boolean,
        val deloadReason: DeloadReason?,
        val phaseLabel: String  // "Volume phase", "Intensity phase", "Deload · Recovery"
    )

    enum class DeloadReason { SCHEDULED, PLATEAU, EXTENDED_ABSENCE }

    fun computeState(
        blockStartDate: Date,
        blockNumber: Int,
        lastWorkoutDate: Date?,
        plateauedExerciseCount: Int,
        now: Date = Date()
    ): BlockState
}
```

Rules:
- Extended absence: if `lastWorkoutDate != null && daysSince(lastWorkoutDate) >= 14` → return week 1 of a new block (reason: EXTENDED_ABSENCE)
- Week calculation: `weeksSinceStart = daysSinceStart / 7`; `weekInBlock = (weeksSinceStart % 4) + 1`
- If `weekInBlock == 4` → deload (reason: SCHEDULED)
- Else if `plateauedExerciseCount >= 2` → force deload (reason: PLATEAU)
- Phase labels:
  - Week 1: "Volume phase · Baseline"
  - Week 2: "Volume phase · Add reps"
  - Week 3: "Intensity phase · Add weight"
  - Week 4: "Deload · Recovery" (scheduled), "Deload · Plateau recovery" (plateau), "Deload · Returning" (absence)

### 3. Block State Repository

New file: `app/src/main/java/com/workoutapp/data/repository/BlockStateRepository.kt`

Wraps SharedPreferences reads/writes. Injected via Hilt.

```kotlin
interface BlockStateRepository {
    suspend fun getState(gymId: Long): Pair<Date, Int>  // (blockStartDate, blockNumber)
    suspend fun setState(gymId: Long, blockStartDate: Date, blockNumber: Int)
    suspend fun advanceBlock(gymId: Long)  // sets start to next Monday, increments number
}
```

### 4. Weekly Progression Logic

Modify `StrengthSetPrescriber.kt` to accept `weekInBlock` and adjust prescription:

```kotlin
fun prescribe(
    positionInWorkout: Int,
    history: List<WorkoutExercise>,
    equipment: String = "",
    progressionRate: Float? = null,
    weekInBlock: Int = 1,      // NEW: 1-4, default 1 (baseline)
    isDeloadWeek: Boolean = false  // NEW
): StrengthPrescription
```

Progression by week (applies to position 0 anchor + position 1 hypertrophy only; accessory position 2+ uses existing logic):

| Week | Behavior |
|------|----------|
| 1 | Baseline: targetReps = exerciseRange (e.g., 6-8) |
| 2 | Add reps: targetRepsMax + 1 (capped at +1, skip if targetRepsMax ≤ 5). Weight stays same. |
| 3 | Add weight: if cleared reps last session AND equipment allows +5lb jump, recommend +5lb at baseline rep range. This layers on top of Phase 1's equipment-aware logic (DB slow progressors still get rep progression instead). |
| 4 | Deload: weight × 0.80 (round to nearest 5), sets = originalSets - 1 (min 2). Rep range stays the same. |

### 5. Plateau Detection Integration

In `GenerateWorkoutUseCase.invoke()`, after selecting exercises:

```kotlin
val plateauedCount = selected.count { exercise ->
    exerciseProfiles[exercise.id]?.plateauFlag == true
}
val blockState = BlockPeriodization.computeState(
    blockStartDate = blockStart,
    blockNumber = blockNumber,
    lastWorkoutDate = lastWorkout?.date,
    plateauedExerciseCount = plateauedCount
)
```

Pass `blockState.weekInBlock` and `blockState.isDeloadWeek` through to `WorkoutViewModel`, which passes them to `StrengthSetPrescriber.prescribe()`.

### 6. Block Advance on Workout Completion

In `WorkoutViewModel.completeWorkout()`, after marking workout complete:

```kotlin
// If this was the deload week (or week 4), advance the block
val state = BlockPeriodization.computeState(...)
if (state.weekInBlock == 4) {
    blockStateRepository.advanceBlock(gymId)
}
```

"Advance" means:
- `block_start_date = next Monday after today`
- `block_number += 1`

Extended absence reset is computed on generation time, not completion. The next completed workout naturally advances from week 1 of the new block.

### 7. UI: Home Screen Block Indicator

Modify `HomeScreen.kt` / `HomeViewModel.kt`:

Add a new state field: `blockIndicator: String?` in the home UI state.

On gym selection (for LMU Gym only), compute current block state and produce label:
- Week 1: "Week 1 of 4 · Volume phase · Baseline"
- Week 2: "Week 2 of 4 · Volume phase · Add reps"
- Week 3: "Week 3 of 4 · Intensity phase · Add weight"
- Week 4 scheduled: "Deload week · Recovery"
- Plateau-triggered: "Deload week · Plateau recovery"
- Extended absence: "Week 1 · Returning to training"

Display as small caption under the "Today's Hunt" card title. Hide for conditioning gyms.

### 8. Dependency Injection

Add `BlockStateRepository` binding to `RepositoryModule.kt`. Follow existing Hilt patterns.

## Architecture

```
User taps "Begin the Hunt"
  ↓
HomeViewModel
  ↓
GenerateWorkoutUseCase
  → BlockStateRepository.getState(gymId) → (startDate, blockNumber)
  → generates exercises (Phase 1 logic unchanged)
  → BlockPeriodization.computeState(...) → BlockState
  ↓
WorkoutViewModel (receives exercises + blockState)
  ↓
buildPrescriptions()
  → StrengthSetPrescriber.prescribe(weekInBlock, isDeloadWeek, ...)
  ↓
User completes workout
  ↓
WorkoutViewModel.completeWorkout()
  → If week 4, BlockStateRepository.advanceBlock(gymId)
```

## Verification

### Block state computation
- Day 0: week 1 ("Volume phase")
- Day 7: week 2
- Day 14: week 3 ("Intensity phase")
- Day 21: week 4 ("Deload · Recovery")
- Day 28: week 1 of next block (after completing a week 4 workout)
- Day 0 with 14+ day gap since last workout: "Week 1 · Returning to training"

### Weekly progression
- Week 1 anchor: 4x6-8 @ 190lb
- Week 2 anchor: 4x6-9 @ 190lb (same weight, chase rep)
- Week 3 anchor: 4x6-8 @ 195lb (if cleared reps last session)
- Week 4 anchor: 3x6-8 @ 155lb (rounded from 195 × 0.80)

### Plateau deload
- Seed 2 exercises with plateauFlag=true
- Generate a workout on week 2 → should still deload (force-override)
- Rationale should say "plateau recovery"

### Extended absence
- Last workout date = 20 days ago
- Generate workout → week 1 "Returning to training"
- After completing it, next workout advances normally (week 2)

### Accessory unaffected
- Position 2+ exercises (accessories) use Phase 1 logic regardless of block week
- Week 3 accessory = same as week 1 accessory (no intensity bump)

### Logcat
- `adb logcat -s FortisLupus` should show block state on each generation:
  - "Block 3 Week 2 (scheduled, plateauCount=0)"
  - "Deload triggered (plateau, count=2)"

## Risks

- **SharedPreferences is per-app, not synced**: if user installs on a new device, block state resets. Acceptable for single-device use.
- **Calendar-based counting doesn't handle daylight saving edge case**: `Date` arithmetic in Calendar.getInstance() handles DST transitions. No special case needed.
- **Plateau flag recomputed per workout**: could theoretically flip between generations. ProfileComputerUseCase runs after each completed workout, so state is stable between "start workout" and "complete workout."
- **Week 3 +5lb could compound with Phase 1's equipment-aware logic**: Phase 2's week 3 rule ONLY fires if Phase 1's logic would ALSO recommend +5lb. No double-counting.

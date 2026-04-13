# Smart Workout Recommendations — Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace random workout generation with trainer-informed selection: conditioning format alternation, exercise freshness decay, bench press priority, equipment-aware weight progression, and empty pool safety net.

**Architecture:** A shared `ExerciseFreshness` utility computes selection weights from last-performed dates. Each generator (strength + conditioning) uses it independently. The prescriber gains equipment awareness via an additional parameter. Bench press is identified by runtime name lookup, not hardcoded ID.

**Tech Stack:** Kotlin, Room DAO queries, existing MVVM architecture

**Non-Goals:** Periodization (Phase 2), cross-gym fatigue (Phase 3), RPE/preference tracking (Phase 4). No schema changes or migrations. No INIT_VERSION bump — this change is pure logic, no seeding.

**Freshness Scope:** Global across all gyms and formats. If you did barbell rows at LMU yesterday, they're deprioritized everywhere. This matches how a trainer thinks — your muscles don't know which gym you're at.

**Rollback:** All changes are in workout generation logic (pure functions). No database schema changes, no migration, no persistent state. Reverting the code restores previous behavior with zero data impact.

---

## File Map

| File | Action | Responsibility |
|------|--------|---------------|
| `data/database/dao/WorkoutDao.kt` | Modify | New query: exercise last-performed dates |
| `data/repository/WorkoutRepositoryImpl.kt` | Modify | Expose last-performed dates |
| `domain/repository/WorkoutRepository.kt` | Modify | Interface additions |
| `domain/usecase/ExerciseFreshness.kt` | Create | Shared freshness weight calculator + weighted random selection |
| `domain/usecase/GenerateWorkoutUseCase.kt` | Modify | Freshness-weighted selection, bench press priority, empty pool fallback |
| `domain/usecase/GenerateConditioningWorkoutUseCase.kt` | Modify | Format alternation, freshness-weighted bucket picks |
| `domain/usecase/StrengthSetPrescriber.kt` | Modify | Equipment-aware progression delta |
| `presentation/viewmodel/HomeViewModel.kt` | Modify | Update predictNextFormat caller (now suspend + gymId) |
| `presentation/viewmodel/WorkoutViewModel.kt` | Modify | Pass equipment + progressionRate to prescriber |

**Risks:**
- Bench press name may vary between ExerciseDataV2 ("Barbell Bench Press") and Hevy imports ("Bench Press (Barbell)"). Mitigated by trying both names at runtime.
- Freshness query on large history could be slow. Mitigated by the GROUP BY + MAX aggregate — single pass, one row per exercise.
- Converting predictNextFormat to suspend changes its contract. Mitigated by default parameter (backwards-compatible) and updating the single caller in HomeViewModel.

---

### Task 1: Add exercise last-performed dates DAO query

**Files:**
- Modify: `app/src/main/java/com/workoutapp/data/database/dao/WorkoutDao.kt`
- Modify: `app/src/main/java/com/workoutapp/domain/repository/WorkoutRepository.kt`
- Modify: `app/src/main/java/com/workoutapp/data/repository/WorkoutRepositoryImpl.kt`

- [ ] **Step 1: Add DAO query for exercise last-performed dates**

In `WorkoutDao.kt`, add after the existing `getExerciseIdsFromDate` query:

```kotlin
@Query("""
    SELECT we.exerciseId, MAX(w.date) as lastDate
    FROM workout_exercises we
    INNER JOIN workouts w ON we.workoutId = w.id
    WHERE w.status = 'COMPLETED'
    GROUP BY we.exerciseId
""")
suspend fun getExerciseLastPerformedDates(): List<ExerciseLastPerformed>
```

Add the return type as a data class in the same file (Room supports POJOs for query results):

```kotlin
data class ExerciseLastPerformed(
    val exerciseId: String,
    val lastDate: Date
)
```

- [ ] **Step 2: Add repository interface method**

In `WorkoutRepository.kt`, add:

```kotlin
suspend fun getExerciseLastPerformedDates(): Map<String, Date>
```

- [ ] **Step 3: Implement in repository**

In `WorkoutRepositoryImpl.kt`, add:

```kotlin
override suspend fun getExerciseLastPerformedDates(): Map<String, Date> {
    return workoutDao.getExerciseLastPerformedDates()
        .associate { it.exerciseId to it.lastDate }
}
```

- [ ] **Step 4: Compile and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/workoutapp/data/database/dao/WorkoutDao.kt \
       app/src/main/java/com/workoutapp/domain/repository/WorkoutRepository.kt \
       app/src/main/java/com/workoutapp/data/repository/WorkoutRepositoryImpl.kt
git commit -m "feat: Add exercise last-performed dates query for freshness decay"
```

---

### Task 2: Create ExerciseFreshness utility

**Files:**
- Create: `app/src/main/java/com/workoutapp/domain/usecase/ExerciseFreshness.kt`

- [ ] **Step 1: Create the freshness weight calculator**

Create `ExerciseFreshness.kt`:

```kotlin
package com.workoutapp.domain.usecase

import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Computes selection weights for exercises based on days since last performed.
 * Used by both LMU strength and Home Gym conditioning generators.
 *
 * Weight table (from 10x trainer analysis):
 *   0-2 days:   0.05 (nearly excluded — still recovering)
 *   3-4 days:   0.30 (short window, deprioritize)
 *   5-7 days:   1.00 (optimal rotation — full priority)
 *   8-14 days:  1.10 (slightly overdue, mild boost)
 *   14+ days:   1.00 (normal weight)
 *   never done: 1.00 (eligible, no boost)
 */
object ExerciseFreshness {

    fun weight(exerciseId: String, lastPerformedDates: Map<String, Date>, now: Date = Date()): Double {
        val lastDate = lastPerformedDates[exerciseId] ?: return 1.0 // never done
        val daysSince = TimeUnit.MILLISECONDS.toDays(now.time - lastDate.time).toInt()
        return when {
            daysSince <= 2 -> 0.05
            daysSince <= 4 -> 0.30
            daysSince <= 7 -> 1.00
            daysSince <= 14 -> 1.10
            else -> 1.00
        }
    }

    fun daysSinceLastPerformed(exerciseId: String, lastPerformedDates: Map<String, Date>, now: Date = Date()): Int? {
        val lastDate = lastPerformedDates[exerciseId] ?: return null
        return TimeUnit.MILLISECONDS.toDays(now.time - lastDate.time).toInt()
    }

    /**
     * Weighted random selection from a list of candidates.
     * Each candidate's probability is proportional to its weight.
     * Returns null if candidates is empty or all weights are zero.
     */
    fun <T> weightedRandom(
        candidates: List<T>,
        weightFn: (T) -> Double,
        random: Random = Random.Default
    ): T? {
        if (candidates.isEmpty()) return null
        val weights = candidates.map { weightFn(it).coerceAtLeast(0.0) }
        val totalWeight = weights.sum()
        if (totalWeight <= 0.0) return null

        var roll = random.nextDouble() * totalWeight
        for (i in candidates.indices) {
            roll -= weights[i]
            if (roll <= 0.0) return candidates[i]
        }
        return candidates.last() // floating point safety
    }
}
```

- [ ] **Step 2: Compile and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/workoutapp/domain/usecase/ExerciseFreshness.kt
git commit -m "feat: Add ExerciseFreshness utility for weighted exercise selection"
```

---

### Task 3: Conditioning format alternation

**Files:**
- Modify: `app/src/main/java/com/workoutapp/domain/usecase/GenerateConditioningWorkoutUseCase.kt`

- [ ] **Step 1: Change predictNextFormat to use history**

Replace the existing `predictNextFormat()` (line 54-55) and update the class to use `workoutRepository` for format resolution.

Change `predictNextFormat()` from:

```kotlin
fun predictNextFormat(): WorkoutFormat =
    if (Random.nextBoolean()) WorkoutFormat.EMOM else WorkoutFormat.AMRAP
```

To a suspend function with gym-aware alternation:

```kotlin
suspend fun predictNextFormat(gymId: Long): WorkoutFormat {
    val lastWorkout = workoutRepository.getLastCompletedWorkoutByGym(gymId)
    if (lastWorkout == null) return WorkoutFormat.EMOM // no history → structured first

    // Week reset: first conditioning workout after Monday boundary → EMOM
    val weekStart = currentWeekStartMonday()
    if (lastWorkout.date.before(weekStart)) return WorkoutFormat.EMOM

    // Alternate from last format
    return when (lastWorkout.format) {
        WorkoutFormat.EMOM -> WorkoutFormat.AMRAP
        WorkoutFormat.AMRAP -> WorkoutFormat.EMOM
        else -> WorkoutFormat.EMOM // strength workout at this gym → start with EMOM
    }
}

private fun currentWeekStartMonday(): Date {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val daysFromMonday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
    return cal.time
}
```

Add imports at top of file:

```kotlin
import java.util.Calendar
import java.util.Date
```

- [ ] **Step 2: Update invoke() to use new predictNextFormat**

In `invoke()` (line 63), change:

```kotlin
val format = formatOverride ?: predictNextFormat()
```

To:

```kotlin
val format = formatOverride ?: predictNextFormat(gymId)
```

- [ ] **Step 3: Update callers of predictNextFormat**

Search for all callers of `predictNextFormat()`. The HomeViewModel calls it for the NextWorkoutCard preview. Update to pass gymId:

In `HomeViewModel.kt`, find where `predictNextFormat()` is called and change to `predictNextFormat(gymId)`. Since it's now a suspend function, ensure it's called from a coroutine context.

- [ ] **Step 4: Compile and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/workoutapp/domain/usecase/GenerateConditioningWorkoutUseCase.kt \
       app/src/main/java/com/workoutapp/presentation/viewmodel/HomeViewModel.kt
git commit -m "feat: Replace random conditioning format with history-aware alternation"
```

---

### Task 4: LMU strength freshness-weighted selection + bench press priority

**Files:**
- Modify: `app/src/main/java/com/workoutapp/domain/usecase/GenerateWorkoutUseCase.kt`

- [ ] **Step 1: Replace binary cooldown with freshness-weighted selection**

In `GenerateWorkoutUseCase.kt`, replace the current approach. Change the exercise loading section.

Replace (around lines 43-57):

```kotlin
val recentExerciseIds = workoutRepository.getExerciseIdsFromLastWeek()
```

With:

```kotlin
val lastPerformedDates = workoutRepository.getExerciseLastPerformedDates()
```

Remove the `filterNot` line (line 57):

```kotlin
.filterNot { it.id in recentExerciseIds }
```

The available exercises should no longer be hard-filtered. Instead, all equipment-compatible exercises enter the pool and freshness weighting handles prioritization.

- [ ] **Step 2: Replace randomOrNull with weighted selection + bench press priority**

Replace the `mapNotNull` exercise selection block (lines 73-91) with:

```kotlin
// Bench press priority: force-select if not done in 14+ days (push day, chest slot only)
// Resolve bench press by name at runtime — ID varies between ExerciseDataV2 and Hevy imports
val benchPress = availableExercises.find {
    it.name.contains("Bench Press", ignoreCase = true) && it.equipment.contains("Barbell", ignoreCase = true)
}
val benchPressDaysSince = benchPress?.let {
    ExerciseFreshness.daysSinceLastPerformed(it.id, lastPerformedDates)
}
val benchPressOverdue = benchPress != null && (
    (benchPressDaysSince != null && benchPressDaysSince >= 14) ||
    (benchPressDaysSince == null && lastPerformedDates.isNotEmpty()) // has history but never benched
)

// Track if a new exercise (never performed) has been selected this workout
var newExerciseUsed = false

val selected = targetMuscleGroups.mapNotNull { muscleGroup ->
    val candidates = exercisesByMuscle[muscleGroup] ?: return@mapNotNull null
    val filtered = when (muscleGroup) {
        MuscleGroup.LEGS -> candidates
            .filter { it.id in LmuLegCatalog.allowedIds }
            .ifEmpty { candidates }
        else -> candidates
    }

    // Bench press priority: force into chest slot if overdue
    if (muscleGroup == MuscleGroup.CHEST && workoutType == WorkoutType.PUSH && benchPressOverdue && benchPress != null) {
        val bench = filtered.find { it.id == benchPress.id }
        if (bench != null) {
            Log.d(TAG, "Force-selected bench press for CHEST (${benchPressDaysSince ?: "never"}d since last)")
            return@mapNotNull bench
        }
    }

    // Deprioritize plateau'd exercises
    val nonPlateau = filtered.filter { exercise ->
        val profile = exerciseProfiles[exercise.id]
        profile == null || !profile.plateauFlag
    }
    val pool = nonPlateau.ifEmpty { filtered }

    // Cap new exercises: max 1 per workout, only in accessory position (not position 0)
    val position = targetMuscleGroups.indexOf(muscleGroup)
    val poolForSelection = if (position == 0 || newExerciseUsed) {
        pool.filter { it.id in lastPerformedDates } .ifEmpty { pool }
    } else {
        pool
    }

    // Weighted random selection based on freshness
    val picked = ExerciseFreshness.weightedRandom(poolForSelection) { exercise ->
        ExerciseFreshness.weight(exercise.id, lastPerformedDates)
    }

    // Fallback: if weighted selection returned null, try full pool ignoring freshness
    val result = picked ?: run {
        Log.w(TAG, "Freshness fallback for $muscleGroup — weighted pool exhausted")
        pool.randomOrNull()
    }

    result?.also {
        if (it.id !in lastPerformedDates) newExerciseUsed = true
        val days = ExerciseFreshness.daysSinceLastPerformed(it.id, lastPerformedDates)
        val profile = exerciseProfiles[it.id]
        Log.d(TAG, "Selected ${it.name} for $muscleGroup (pool=${poolForSelection.size}, days=${days ?: "new"}, plateau=${profile?.plateauFlag ?: false})")
    }
}.take(3)
```

Add import at top:

```kotlin
import com.workoutapp.domain.usecase.ExerciseFreshness
```

- [ ] **Step 3: Remove the old getExerciseIdsFromLastWeek call**

The old `recentExerciseIds` variable and the `getExerciseIdsFromLastWeek()` call are no longer used in this file. Remove them. (The method still exists on the repository for other potential callers.)

- [ ] **Step 4: Compile and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/workoutapp/domain/usecase/GenerateWorkoutUseCase.kt
git commit -m "feat: Freshness-weighted exercise selection with bench press priority"
```

---

### Task 5: Conditioning exercise freshness

**Files:**
- Modify: `app/src/main/java/com/workoutapp/domain/usecase/GenerateConditioningWorkoutUseCase.kt`

- [ ] **Step 1: Inject WorkoutRepository (if not already) and load last-performed dates**

The class already has `workoutRepository` injected. At the start of `pickMovementIds()`, load the dates:

Change `pickMovementIds` from a private function to use the repository data. Since `pickMovementIds` is not a suspend function but needs the dates, load them in `invoke()` and pass them through.

In `invoke()`, before `pickMovementIds` is called (around line 71), add:

```kotlin
val lastPerformedDates = workoutRepository.getExerciseLastPerformedDates()
```

Change `pickMovementIds` signature to accept dates:

```kotlin
private fun pickMovementIds(format: WorkoutFormat, lastPerformedDates: Map<String, Date>): List<String>
```

Update the call site in `invoke()`:

```kotlin
var candidateIds = pickMovementIds(format, lastPerformedDates)
```

And in the retry loop:

```kotlin
candidateIds = pickMovementIds(format, lastPerformedDates)
```

- [ ] **Step 2: Replace .random() with freshness-weighted selection in EMOM path**

In the EMOM section of `pickMovementIds`, replace each `.random()` call with `ExerciseFreshness.weightedRandom`:

```kotlin
WorkoutFormat.EMOM -> {
    val legsPick = ExerciseFreshness.weightedRandom(lower) {
        ExerciseFreshness.weight(it.id, lastPerformedDates)
    } ?: lower.random()

    val pullPool = if (legsPick.isHeavy()) pull.filterNot { it.isHeavy() } else pull
    val pullPick = ExerciseFreshness.weightedRandom(pullPool.ifEmpty { pull }) {
        ExerciseFreshness.weight(it.id, lastPerformedDates)
    } ?: pullPool.ifEmpty { pull }.random()

    val heavyUsed = legsPick.isHeavy() || pullPick.isHeavy()
    val pushPool = if (heavyUsed) push.filterNot { it.isHeavy() } else push
    val pushPick = ExerciseFreshness.weightedRandom(pushPool.ifEmpty { push }) {
        ExerciseFreshness.weight(it.id, lastPerformedDates)
    } ?: pushPool.ifEmpty { push }.random()

    val closerPick = ExerciseFreshness.weightedRandom(core + conditioning) {
        ExerciseFreshness.weight(it.id, lastPerformedDates)
    } ?: (core + conditioning).random()

    val picks = mutableListOf(legsPick, pullPick, pushPick, closerPick)
    capTrx(picks, listOf(lower, pull, push, core + conditioning))
    picks.map { it.id }
}
```

- [ ] **Step 3: Replace .random() with freshness-weighted selection in AMRAP path**

```kotlin
WorkoutFormat.AMRAP -> {
    val cardioPick = ExerciseFreshness.weightedRandom(cardio) {
        ExerciseFreshness.weight(it.id, lastPerformedDates)
    } ?: cardio.random()

    val strengthPool = lower + pull + push
    val strengthPick = ExerciseFreshness.weightedRandom(strengthPool) {
        ExerciseFreshness.weight(it.id, lastPerformedDates)
    } ?: strengthPool.random()

    val closerPool = core + conditioning
    val closerPick = ExerciseFreshness.weightedRandom(closerPool) {
        ExerciseFreshness.weight(it.id, lastPerformedDates)
    } ?: closerPool.random()

    val picks = mutableListOf(cardioPick, strengthPick, closerPick)
    capTrx(picks, listOf(cardio, lower + pull + push, core + conditioning))
    picks.map { it.id }
}
```

Add import:

```kotlin
import com.workoutapp.domain.usecase.ExerciseFreshness
import java.util.Date
```

- [ ] **Step 4: Compile and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/workoutapp/domain/usecase/GenerateConditioningWorkoutUseCase.kt
git commit -m "feat: Freshness-weighted conditioning exercise selection"
```

---

### Task 6: Equipment-aware weight progression

**Files:**
- Modify: `app/src/main/java/com/workoutapp/domain/usecase/StrengthSetPrescriber.kt`

- [ ] **Step 1: Add equipment parameter to prescribe()**

Change the `prescribe()` signature (line 51-54) from:

```kotlin
fun prescribe(
    positionInWorkout: Int,
    history: List<WorkoutExercise>
): StrengthPrescription
```

To:

```kotlin
fun prescribe(
    positionInWorkout: Int,
    history: List<WorkoutExercise>,
    equipment: String = "",
    progressionRate: Float? = null
): StrengthPrescription
```

- [ ] **Step 2: Replace hardcoded +5lb with equipment-aware progression**

Replace the progression block (lines 106-117). Change from:

```kotlin
recommendedWeight = mostRecentWorkingWeight + PROGRESSION_DELTA_LB,
rationale = "+${PROGRESSION_DELTA_LB.toInt()} lb — cleared ${template.repsMax} reps last 2 sessions"
```

To:

```kotlin
recommendedWeight = if (isDumbbellSlowProgressor(equipment, progressionRate)) {
    mostRecentWorkingWeight // hold weight, bump reps instead
} else {
    mostRecentWorkingWeight + PROGRESSION_DELTA_LB
},
rationale = if (isDumbbellSlowProgressor(equipment, progressionRate)) {
    "hold ${mostRecentWorkingWeight.toInt()} lb — aim for ${template.repsMax + 1} reps to earn +${PROGRESSION_DELTA_LB.toInt()} lb"
} else {
    "+${PROGRESSION_DELTA_LB.toInt()} lb — cleared ${template.repsMax} reps last 2 sessions"
},
targetRepsMax = if (isDumbbellSlowProgressor(equipment, progressionRate)) {
    template.repsMax + 1 // bump rep target by 1
} else {
    template.repsMax
}
```

Note: `StrengthPrescription` may need a `targetRepsMax` override. Check the data class. If `targetRepsMax` is already in StrengthPrescription, use it. If not, keep the existing `template.repsMax` in the return and add the rep bump to the rationale only (the user reads the rationale and adjusts).

Add the helper method:

```kotlin
private fun isDumbbellSlowProgressor(equipment: String, progressionRate: Float?): Boolean {
    if (equipment.lowercase() != "dumbbell") return false
    return progressionRate != null && progressionRate < 5f
}
```

- [ ] **Step 3: Update callers to pass equipment and progressionRate**

In `WorkoutViewModel.kt`, find where `StrengthSetPrescriber.prescribe()` is called. It's called in `buildPrescriptions()`. Update to pass the exercise's equipment and profile's progressionRate:

```kotlin
val prescription = StrengthSetPrescriber.prescribe(
    positionInWorkout = index,
    history = exerciseHistory,
    equipment = exercise.exercise.equipment,
    progressionRate = profiles[exercise.exercise.id]?.progressionRateLbPerMonth
)
```

- [ ] **Step 4: Compile and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/workoutapp/domain/usecase/StrengthSetPrescriber.kt \
       app/src/main/java/com/workoutapp/presentation/viewmodel/WorkoutViewModel.kt
git commit -m "feat: Equipment-aware weight progression (DB rep progression for slow progressors)"
```

---

### Task 7: Build and verify

- [ ] **Step 1: Full build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify with logcat**

After installing on device, check logcat for the new freshness and format logs:

```bash
adb logcat -s FortisLupus
```

Expected output should include:
- "Force-selected bench press for CHEST" (if bench is 14+ days old)
- "Selected [name] for [muscle] (pool=N, days=M, plateau=false)"
- "Freshness fallback for [muscle]" (only if pool exhaustion occurs)

No INIT_VERSION bump needed — this change is pure generation logic with no seeding changes.

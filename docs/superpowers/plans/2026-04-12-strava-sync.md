# Strava Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the Strava sync pipeline for both strength and conditioning workouts, including the WorkManager-based sync worker, conditioning description formatting, and auto-sync on workout completion.

**Architecture:** Queue-based sync with WorkManager. On workout completion, insert a PENDING row into the existing `strava_sync_queue` table and enqueue a `StravaSyncWorker`. The worker processes pending items, maps workouts to Strava API requests (format-aware: strength vs EMOM/AMRAP), and posts them with retry logic.

**Tech Stack:** Kotlin, Hilt DI, WorkManager, Retrofit, Room

**Spec:** `docs/superpowers/specs/2026-04-12-strava-sync-design.md`

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `app/build.gradle.kts` | Modify | Add hilt-work dependency |
| `app/src/main/AndroidManifest.xml` | Modify | Remove default WorkManager initializer |
| `app/src/main/java/com/workoutapp/WorkoutApplication.kt` | Modify | HiltWorkerFactory + Configuration.Provider |
| `app/src/main/java/com/workoutapp/domain/formatter/StravaDescriptionFormatter.kt` | Modify | Add conditioning format path |
| `app/src/main/java/com/workoutapp/domain/mapper/WorkoutToStravaMapper.kt` | Modify | Add conditioning activity type/name branch |
| `app/src/main/java/com/workoutapp/data/sync/StravaSyncManager.kt` | Create | Queue insertion + WorkManager enqueue |
| `app/src/main/java/com/workoutapp/data/sync/StravaSyncWorker.kt` | Create | @HiltWorker that processes sync queue |
| `app/src/main/java/com/workoutapp/presentation/viewmodel/WorkoutViewModel.kt` | Modify | Inject StravaSyncManager, call on completion |
| `app/src/main/java/com/workoutapp/presentation/viewmodel/ConditioningWorkoutViewModel.kt` | Modify | Inject StravaSyncManager, call on completion |

---

### Task 1: Add hilt-work dependency and configure WorkManager

**Files:**
- Modify: `app/build.gradle.kts:104` (after existing hilt deps)
- Modify: `app/src/main/AndroidManifest.xml:7` (inside `<application>` tag)
- Modify: `app/src/main/java/com/workoutapp/WorkoutApplication.kt`

- [ ] **Step 1: Add hilt-work dependency to build.gradle.kts**

After the line `implementation("androidx.hilt:hilt-navigation-compose:1.1.0")`, add:

```kotlin
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")
```

- [ ] **Step 2: Remove default WorkManager initializer in AndroidManifest.xml**

Inside the `<application>` tag, before `<activity`, add:

```xml
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            tools:node="remove" />
```

- [ ] **Step 3: Add HiltWorkerFactory to WorkoutApplication.kt**

Add imports:

```kotlin
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
```

Change the class declaration from:

```kotlin
class WorkoutApplication : Application() {
```

to:

```kotlin
class WorkoutApplication : Application(), Configuration.Provider {
```

Add the worker factory field after the existing `@Inject` fields:

```kotlin
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
```

Add the Configuration.Provider implementation after the `companion object`:

```kotlin
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
```

- [ ] **Step 4: Build to verify dependencies resolve**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/com/workoutapp/WorkoutApplication.kt
git commit -m "feat: Add hilt-work dependency and configure custom WorkManager initializer"
```

---

### Task 2: Extend StravaDescriptionFormatter with conditioning support

**Files:**
- Modify: `app/src/main/java/com/workoutapp/domain/formatter/StravaDescriptionFormatter.kt`

- [ ] **Step 1: Add imports for WorkoutFormat and RepPrescriber**

Add to the import block:

```kotlin
import com.workoutapp.domain.model.WorkoutFormat
import com.workoutapp.domain.usecase.RepPrescriber
```

- [ ] **Step 2: Update format() to branch on workout.format**

Replace the existing `format()` method body (lines 38-64) with:

```kotlin
    fun format(
        workout: Workout,
        startTime: Long? = null,
        endTime: Long? = null
    ): String {
        return when (workout.format) {
            WorkoutFormat.EMOM, WorkoutFormat.AMRAP -> formatConditioning(workout)
            else -> formatStrength(workout, startTime, endTime)
        }
    }
```

- [ ] **Step 3: Rename existing format logic to formatStrength()**

Add a new private method containing the original format body:

```kotlin
    private fun formatStrength(
        workout: Workout,
        startTime: Long? = null,
        endTime: Long? = null
    ): String {
        return buildString {
            appendLine(formatHeader(workout.type))
            appendLine()

            val exercisesByMuscle = groupExercisesByMuscleGroup(workout.exercises)

            exercisesByMuscle.forEach { (muscleGroup, exercises) ->
                appendLine("${muscleGroup.displayName}:")
                exercises.forEach { workoutExercise ->
                    appendLine(formatExercise(workoutExercise))
                }
                appendLine()
            }

            val totalVolume = calculateTotalVolume(workout.exercises)
            appendLine("Total Volume: ${formatVolume(totalVolume)}")

            if (startTime != null && endTime != null) {
                val durationMinutes = calculateDuration(startTime, endTime)
                appendLine("Duration: $durationMinutes minutes")
            }
        }.trim()
    }
```

- [ ] **Step 4: Add formatConditioning() method**

```kotlin
    private fun formatConditioning(workout: Workout): String {
        val sessionIds = workout.exercises.map { it.exercise.id }

        return buildString {
            // Header: 🔥 AMRAP Workout (15 min)
            appendLine("🔥 ${workout.format.name} Workout (${workout.durationMinutes} min)")
            appendLine()

            // Exercise list with rep prescriptions
            appendLine("Exercises:")
            workout.exercises.forEach { we ->
                val prescription = RepPrescriber.prescribe(
                    exerciseId = we.exercise.id,
                    format = workout.format,
                    sessionIds = sessionIds
                )
                // Special displays (e.g. "200m row") already include context
                val display = if (prescription.contains("m row") || prescription.contains("m Row")) {
                    "• ${prescription.replaceFirstChar { it.uppercase() }}"
                } else {
                    "• ${we.exercise.name} ($prescription)"
                }
                appendLine(display)
            }
            appendLine()

            // AMRAP: show completed rounds. EMOM: show duration.
            if (workout.format == WorkoutFormat.AMRAP && workout.completedRounds != null) {
                appendLine("Completed: ${workout.completedRounds} rounds")
            } else {
                appendLine("Duration: ${workout.durationMinutes} minutes")
            }
            appendLine()
            appendLine("Logged with Fortis Lupus \uD83D\uDC3A")
        }.trim()
    }
```

- [ ] **Step 5: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/workoutapp/domain/formatter/StravaDescriptionFormatter.kt
git commit -m "feat: Add conditioning workout description formatting for Strava"
```

---

### Task 3: Extend WorkoutToStravaMapper with conditioning support

**Files:**
- Modify: `app/src/main/java/com/workoutapp/domain/mapper/WorkoutToStravaMapper.kt`

- [ ] **Step 1: Add WorkoutFormat import**

```kotlin
import com.workoutapp.domain.model.WorkoutFormat
```

- [ ] **Step 2: Update mapToActivityRequest() to branch on format**

Replace the existing `mapToActivityRequest()` body (lines 29-51) with:

```kotlin
    fun mapToActivityRequest(
        workout: Workout,
        startTime: Long? = null,
        endTime: Long? = null
    ): StravaActivityRequest {
        val isConditioning = workout.format == WorkoutFormat.EMOM || workout.format == WorkoutFormat.AMRAP

        val actualStartTime = startTime ?: workout.date.time
        val actualEndTime = if (isConditioning) {
            // Conditioning has exact duration from generation
            actualStartTime + (workout.durationMinutes ?: 20) * 60 * 1000L
        } else {
            endTime ?: (actualStartTime + estimateDuration(workout))
        }

        return StravaActivityRequest(
            name = formatActivityName(workout),
            type = if (isConditioning) "Workout" else "WeightTraining",
            sportType = if (isConditioning) "Workout" else "WeightTraining",
            startDateLocal = formatDateToIso8601(actualStartTime),
            elapsedTime = calculateElapsedTimeSeconds(actualStartTime, actualEndTime),
            description = StravaDescriptionFormatter.format(
                workout = workout,
                startTime = actualStartTime,
                endTime = actualEndTime
            ),
            trainer = false,
            commute = false
        )
    }
```

- [ ] **Step 3: Update formatActivityName() to handle conditioning**

Replace the existing method with:

```kotlin
    private fun formatActivityName(workout: Workout): String {
        return when (workout.format) {
            WorkoutFormat.EMOM -> "🔥 EMOM Workout"
            WorkoutFormat.AMRAP -> "🔥 AMRAP Workout"
            else -> when (workout.type) {
                WorkoutType.PUSH -> "💪 PUSH Workout"
                WorkoutType.PULL -> "🔥 PULL Workout"
            }
        }
    }
```

Update the signature — it now takes `Workout` instead of `WorkoutType`. Also add the import:

```kotlin
import com.workoutapp.domain.model.Workout
```

(Note: `Workout` may already be imported — check first.)

- [ ] **Step 4: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/workoutapp/domain/mapper/WorkoutToStravaMapper.kt
git commit -m "feat: Add conditioning activity type and naming to Strava mapper"
```

---

### Task 4: Create StravaSyncManager

**Files:**
- Create: `app/src/main/java/com/workoutapp/data/sync/StravaSyncManager.kt`

- [ ] **Step 1: Create the sync manager class**

```kotlin
package com.workoutapp.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.workoutapp.data.database.dao.StravaSyncDao
import com.workoutapp.data.database.entities.SyncStatus
import com.workoutapp.data.database.entities.StravaSyncQueueEntity
import com.workoutapp.data.repository.StravaAuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StravaSyncManager @Inject constructor(
    private val stravaSyncDao: StravaSyncDao,
    private val stravaAuthRepository: StravaAuthRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun queueWorkout(workoutId: String) {
        if (!stravaAuthRepository.isAuthenticated()) return
        if (stravaSyncDao.isWorkoutSynced(workoutId)) return

        val entry = StravaSyncQueueEntity(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            status = SyncStatus.PENDING
        )
        stravaSyncDao.insert(entry)

        val workRequest = OneTimeWorkRequestBuilder<StravaSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "strava_sync",
                ExistingWorkPolicy.APPEND,
                workRequest
            )
    }
}
```

- [ ] **Step 2: Build to verify** (will fail until Task 5 creates the worker — that's expected)

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug 2>&1 | tail -5`

Expected: BUILD FAILED (StravaSyncWorker not found yet)

- [ ] **Step 3: Commit (WIP)**

```bash
git add app/src/main/java/com/workoutapp/data/sync/StravaSyncManager.kt
git commit -m "feat: Add StravaSyncManager for queue insertion and WorkManager enqueue"
```

---

### Task 5: Create StravaSyncWorker

**Files:**
- Create: `app/src/main/java/com/workoutapp/data/sync/StravaSyncWorker.kt`

- [ ] **Step 1: Create the sync worker class**

```kotlin
package com.workoutapp.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.workoutapp.data.database.dao.StravaSyncDao
import com.workoutapp.data.database.entities.SyncStatus
import com.workoutapp.data.remote.strava.StravaApiClient
import com.workoutapp.data.repository.StravaAuthRepository
import com.workoutapp.domain.mapper.WorkoutToStravaMapper
import com.workoutapp.domain.repository.WorkoutRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class StravaSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val stravaSyncDao: StravaSyncDao,
    private val workoutRepository: WorkoutRepository,
    private val stravaAuthRepository: StravaAuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pending = stravaSyncDao.getAllPending() +
            stravaSyncDao.getAllFailed().filter { it.retryCount < 3 }
        if (pending.isEmpty()) return Result.success()

        val api = StravaApiClient.api
        var anyFailed = false

        for (entry in pending) {
            stravaSyncDao.updateStatus(
                entry.id, SyncStatus.IN_PROGRESS, System.currentTimeMillis()
            )

            try {
                val workout = workoutRepository.getWorkoutById(entry.workoutId)
                    ?: throw Exception("Workout not found: ${entry.workoutId}")

                val tokenResult = stravaAuthRepository.getValidAccessToken()
                val token = tokenResult.getOrThrow()

                val request = WorkoutToStravaMapper.mapToActivityRequest(workout)
                val response = api.createActivity("Bearer $token", request)

                if (response.isSuccessful) {
                    val activityId = response.body()?.id
                        ?: throw Exception("Empty response body from Strava")
                    stravaSyncDao.markCompleted(
                        entry.id, activityId, System.currentTimeMillis()
                    )
                } else {
                    throw Exception("Strava API error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                stravaSyncDao.markFailed(
                    entry.id, e.message ?: "Unknown error", System.currentTimeMillis()
                )
                if (entry.retryCount < 3) anyFailed = true
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }
}
```

- [ ] **Step 2: Build to verify both manager and worker compile**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/workoutapp/data/sync/StravaSyncWorker.kt
git commit -m "feat: Add StravaSyncWorker for background Strava sync with retry"
```

---

### Task 6: Wire sync trigger into WorkoutViewModel

**Files:**
- Modify: `app/src/main/java/com/workoutapp/presentation/viewmodel/WorkoutViewModel.kt`

- [ ] **Step 1: Add StravaSyncManager import and constructor parameter**

Add import:

```kotlin
import com.workoutapp.data.sync.StravaSyncManager
```

Add to the constructor (after `profileComputerUseCase` parameter):

```kotlin
    private val stravaSyncManager: StravaSyncManager,
```

- [ ] **Step 2: Add queueWorkout call in completeWorkout()**

In the `completeWorkout()` method, after the line:

```kotlin
                profileComputerUseCase.updateAfterWorkout(completedWorkout.id)
```

add:

```kotlin
                stravaSyncManager.queueWorkout(completedWorkout.id)
```

- [ ] **Step 3: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/workoutapp/presentation/viewmodel/WorkoutViewModel.kt
git commit -m "feat: Auto-queue strength workouts for Strava sync on completion"
```

---

### Task 7: Wire sync trigger into ConditioningWorkoutViewModel

**Files:**
- Modify: `app/src/main/java/com/workoutapp/presentation/viewmodel/ConditioningWorkoutViewModel.kt`

- [ ] **Step 1: Add StravaSyncManager import and constructor parameter**

Add import:

```kotlin
import com.workoutapp.data.sync.StravaSyncManager
```

Add to the constructor (after `profileComputerUseCase` parameter):

```kotlin
    private val stravaSyncManager: StravaSyncManager,
```

- [ ] **Step 2: Add queueWorkout call in completeWorkout()**

In the `completeWorkout()` method, after the line:

```kotlin
            profileComputerUseCase.updateAfterWorkout(completed.id)
```

add:

```kotlin
            stravaSyncManager.queueWorkout(completed.id)
```

- [ ] **Step 3: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/workoutapp/presentation/viewmodel/ConditioningWorkoutViewModel.kt
git commit -m "feat: Auto-queue conditioning workouts for Strava sync on completion"
```

---

### Task 8: Final build verification and integration commit

- [ ] **Step 1: Full clean build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify all new/modified files are committed**

Run: `git status`

Expected: clean working tree (no uncommitted changes from tasks 1-7)

- [ ] **Step 3: Review git log for the task commits**

Run: `git log --oneline -8`

Expected: 7 commits from tasks 1-7

---

## Verification (manual, post-implementation)

1. Fresh install on device (clear app data)
2. Connect Strava in settings (OAuth flow)
3. Complete a strength workout (LMU) — check Strava for "💪 PUSH Workout" with muscle group description
4. Complete an AMRAP conditioning workout (Home Gym) — check Strava for "🔥 AMRAP Workout" with exercise list + rounds
5. Complete an EMOM conditioning workout (Home Gym) — check Strava for "🔥 EMOM Workout" with exercise list + duration
6. Airplane mode test: complete a workout offline, reconnect — verify sync catches up
7. Verify no sync when Strava is not connected (fresh install, no OAuth)

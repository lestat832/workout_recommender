# Strava Sync: Complete Pipeline for Strength + Conditioning

## Context

Strava sync infrastructure (OAuth, API client, sync queue table, description formatter) was built in Phase 5 but the actual sync worker was never implemented. Neither strength nor conditioning workouts currently sync to Strava. This spec completes the pipeline for both workout types and extends the description formatter to handle EMOM/AMRAP conditioning workouts with exercise lists, rep prescriptions, and round counts.

## Non-goals

- Sync history backfill (only new workout completions)
- Strava activity updates or deletes (create-only)
- Sync status UI or notifications
- Settings toggle for auto-sync (always on when authenticated)
- Bidirectional sync (Strava -> app)

---

## Architecture

### Sync flow

```
WorkoutViewModel.completeWorkout()
  |
  v
StravaSyncManager.queueWorkout(workoutId)
  |-- check: Strava auth exists? (early exit if not)
  |-- insert PENDING row into strava_sync_queue
  |-- enqueue one-time StravaSyncWorker via WorkManager
  |
  v
StravaSyncWorker.doWork()
  |-- query all PENDING entries from strava_sync_queue
  |-- for each:
  |     load Workout from DB
  |     map to StravaActivityRequest (format-aware)
  |     get valid access token (auto-refresh if expired)
  |     POST to Strava API
  |     mark COMPLETED or FAILED in queue
  |-- return Result.success() or Result.retry()
```

Same flow for ConditioningWorkoutViewModel.completeWorkout().

### New files

| File | Purpose |
|------|---------|
| `app/.../data/sync/StravaSyncManager.kt` | Queue insertion + WorkManager enqueue. Injected into ViewModels. |
| `app/.../data/sync/StravaSyncWorker.kt` | `@HiltWorker CoroutineWorker` that processes the sync queue. |

### Modified files

| File | Change |
|------|--------|
| `app/.../domain/formatter/StravaDescriptionFormatter.kt` | Add conditioning format path with RepPrescriber integration |
| `app/.../domain/mapper/WorkoutToStravaMapper.kt` | Add conditioning activity type ("Workout"), name, and duration handling |
| `app/.../presentation/viewmodel/WorkoutViewModel.kt` | Inject StravaSyncManager, call queueWorkout on completion |
| `app/.../presentation/viewmodel/ConditioningWorkoutViewModel.kt` | Inject StravaSyncManager, call queueWorkout on completion |
| `app/.../WorkoutApplication.kt` | Implement `Configuration.Provider`, inject `HiltWorkerFactory` for custom WorkManager init |
| `app/build.gradle.kts` | Add `androidx.hilt:hilt-work:1.1.0` + ksp compiler |

---

## Description formatting

### Strength (existing, unchanged)

```
💪 PUSH Workout

Chest:
• Barbell Bench Press: 3x10 @ 135 lbs
• Incline Dumbbell Press: 3x12 @ 50 lbs

Shoulders:
• Dumbbell Shoulder Press: 3x10 @ 40 lbs

Total Volume: 12,450 lbs
Duration: 58 minutes
```

Activity type: `WeightTraining`

### AMRAP conditioning

```
🔥 AMRAP Workout (15 min)

Exercises:
• Row 200m row
• DB Goblet Squat (x 5-6)
• TRX Row (x 8-12)

Completed: 5 rounds

Logged with Fortis Lupus 🐺
```

Activity type: `Workout`

### EMOM conditioning

```
🔥 EMOM Workout (20 min)

Exercises:
• DB Goblet Squat (x 6-8)
• Pull-Up (x 5-6)
• Atomic Push-Up (x 10-15)
• Ab Wheel Rollout (x 15-20)

Duration: 20 minutes

Logged with Fortis Lupus 🐺
```

Activity type: `Workout`

### Implementation

`StravaDescriptionFormatter.format()` checks `workout.format`:
- STRENGTH: existing muscle-group-organized path (unchanged)
- EMOM/AMRAP: new `formatConditioning()` method that:
  - builds header with format name and duration
  - lists exercises by name with RepPrescriber.prescribe() output in parens
  - `sessionIds` for RepPrescriber comes from `workout.exercises.map { it.exercise.id }` — available at sync time since `getWorkoutById()` loads exercises
  - for RepPrescriber special displays (row, jump rope), uses the special display directly as it already includes the exercise context (e.g., "200m row")
  - shows "Completed: X rounds" for AMRAP (when completedRounds != null)
  - shows "Duration: X minutes" for EMOM
  - appends "Logged with Fortis Lupus 🐺" footer

---

## Mapper changes

`WorkoutToStravaMapper.mapToActivityRequest()` branches on `workout.format`:

| Field | Strength | Conditioning |
|-------|----------|-------------|
| name | "💪 PUSH Workout" / "🔥 PULL Workout" | "🔥 EMOM Workout" / "🔥 AMRAP Workout" |
| type | "WeightTraining" | "Workout" |
| sportType | "WeightTraining" | "Workout" |
| elapsedTime | calculated from start/end or estimated | `workout.durationMinutes * 60` |
| description | existing formatter | new conditioning formatter |

Duration for conditioning is straightforward — `durationMinutes` is set during generation (EMOM: 20 min, AMRAP: 15 min) and is the actual workout duration, not an estimate.

---

## StravaSyncManager

```kotlin
@Singleton
class StravaSyncManager @Inject constructor(
    private val stravaSyncDao: StravaSyncDao,
    private val stravaAuthRepository: StravaAuthRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun queueWorkout(workoutId: String) {
        // Skip if user never connected Strava
        if (!stravaAuthRepository.isAuthenticated()) return

        // Skip if already queued/synced
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

Key decisions:
- `isAuthenticated()` gates queueing — no point adding to queue without Strava connection
- `isWorkoutSynced()` prevents duplicate queue entries
- `APPEND` adds new work without cancelling in-flight sync
- network constraint means WorkManager waits for connectivity

---

## StravaSyncWorker

```kotlin
@HiltWorker
class StravaSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val stravaSyncDao: StravaSyncDao,
    private val workoutRepository: WorkoutRepository,
    private val stravaAuthRepository: StravaAuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Query both PENDING and retryable FAILED items (retryCount < 3).
        // markFailed() sets status to FAILED, so getAllPending() alone
        // would miss items that need retry.
        val pending = stravaSyncDao.getAllPending() +
            stravaSyncDao.getAllFailed().filter { it.retryCount < 3 }
        if (pending.isEmpty()) return Result.success()

        val api = StravaApiClient.api  // singleton, no DI needed
        var anyFailed = false

        for (entry in pending) {
            stravaSyncDao.updateStatus(entry.id, SyncStatus.IN_PROGRESS, System.currentTimeMillis())

            try {
                val workout = workoutRepository.getWorkoutById(entry.workoutId)
                    ?: throw Exception("Workout not found: ${entry.workoutId}")

                val tokenResult = stravaAuthRepository.getValidAccessToken()
                val token = tokenResult.getOrThrow()

                val request = WorkoutToStravaMapper.mapToActivityRequest(workout)
                val response = api.createActivity("Bearer $token", request)

                stravaSyncDao.markCompleted(entry.id, response.id, System.currentTimeMillis())
            } catch (e: Exception) {
                stravaSyncDao.markFailed(entry.id, e.message, System.currentTimeMillis())
                if (entry.retryCount < 3) anyFailed = true
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }
}
```

Key decisions:
- processes ALL pending items in one pass (batch efficiency)
- per-item error handling — one failure doesn't block others
- retry limit of 3 per item (tracked in DB via retryCount)
- returns Result.retry() if any retryable items remain

---

## Dependency addition

`app/build.gradle.kts`:
```kotlin
// Hilt WorkManager integration
implementation("androidx.hilt:hilt-work:1.1.0")
ksp("androidx.hilt:hilt-compiler:1.1.0")
```

Note: codebase uses ksp, not kapt. The existing Hilt compiler uses `ksp("com.google.dagger:hilt-compiler:2.48")`.

---

## HiltWorkerFactory setup

`WorkoutApplication.kt` must implement `Configuration.Provider` so Hilt can inject dependencies into `@HiltWorker` classes:

```kotlin
@HiltAndroidApp
class WorkoutApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // ... existing fields and onCreate() ...

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

Manifest: add defensive removal of default WorkManager initializer (transitively included by `work-runtime-ktx`):

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    tools:node="remove" />
```

This prevents the default initializer from conflicting with the custom `Configuration.Provider`.

---

## ViewModel integration

### WorkoutViewModel.completeWorkout()

Add after `workoutRepository.updateWorkout(completedWorkout)`:
```kotlin
stravaSyncManager.queueWorkout(completedWorkout.id)
```

StravaSyncManager injected via constructor.

### ConditioningWorkoutViewModel.completeWorkout()

Add after `workoutRepository.updateWorkout(completed)`:
```kotlin
stravaSyncManager.queueWorkout(completed.id)
```

StravaSyncManager injected via constructor.

---

## Existing infrastructure reused

| Component | File | Reuse |
|-----------|------|-------|
| Sync queue table + DAO | `StravaSyncQueueEntity.kt`, `StravaSyncDao.kt` | Insert, query, mark completed/failed |
| Auth repository | `StravaAuthRepository.kt` | `isAuthenticated()`, `getValidAccessToken()` |
| API interface | `StravaApi.kt` | `createActivity()` |
| Activity request model | `StravaModels.kt` | `StravaActivityRequest` |
| Rep prescriber | `RepPrescriber.kt` | `prescribe()` for conditioning descriptions |
| Description formatter | `StravaDescriptionFormatter.kt` | Extend with conditioning path |
| Workout mapper | `WorkoutToStravaMapper.kt` | Extend with conditioning branch |

---

## Verification

1. Build: `./gradlew assembleDebug`
2. Fresh install on device
3. Connect Strava in settings (OAuth flow)
4. Complete a strength workout (LMU) — verify it appears on Strava with muscle group description
5. Complete an AMRAP conditioning workout (Home Gym) — verify it appears on Strava with exercise list + rounds
6. Complete an EMOM conditioning workout (Home Gym) — verify it appears on Strava with exercise list + duration
7. Airplane mode test: complete a workout, turn on wifi — verify sync catches up via WorkManager
8. Verify no sync attempt when Strava is not connected

## Rollback

- revert code changes — queue rows are inert without the worker processing them
- any COMPLETED queue entries reference Strava activity IDs but don't affect local data
- no schema changes — uses existing `strava_sync_queue` table from migration 5->6
- WorkManager work can be cancelled via `WorkManager.getInstance(context).cancelUniqueWork("strava_sync")`

## Risks and mitigations

- **Token expiration during batch**: `getValidAccessToken()` auto-refreshes per call, so each item gets a fresh check
- **Large queue backlog**: unlikely since only new completions queue, but the worker processes all pending in one pass
- **hilt-work version compatibility**: using 1.1.0 which matches Hilt 2.48+ (app's current version)
- **RepPrescriber format mismatch**: RepPrescriber returns display strings like "x 8-10" — these are user-facing and suitable for Strava descriptions as-is

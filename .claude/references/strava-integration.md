# Strava Integration Patterns

**When to load this reference:**
- Working on Strava OAuth authentication
- Implementing workout-to-Strava sync
- Building sync queue or retry logic
- Debugging Strava API integration
- Working on activity formatting

**Load command:** Uncomment `@.claude/references/strava-integration.md` in `.claude/CLAUDE.md`

---

## Overview

**One-Way Sync**: Workout App → Strava
**Trigger**: Auto-sync immediately when workout marked complete
**Activity Type**: Weight Training
**Format**: Detailed description with emojis (Format A)

## Sync Architecture

### Data Flow
1. **Workout Completion** → User marks workout as complete
2. **Queue Creation** → Create `StravaSyncQueue` entry with `PENDING` status
3. **Background Worker** → WorkManager picks up pending sync
4. **Auth Check** → Verify Strava token, refresh if needed
5. **Data Transform** → Convert workout to Strava activity format
6. **API Call** → POST to Strava `/activities` endpoint
7. **Status Update** → Mark as `SYNCED` or `FAILED`
8. **Notification** → Show success/failure notification

### Components
```
┌─────────────────────────────────────────┐
│            Workout App                  │
├─────────────────────────────────────────┤
│                                         │
│  ┌──────────┐     ┌────────────────┐   │
│  │ Workout  │────→│ Strava Sync    │   │
│  │Complete  │     │ Queue (Room)   │   │
│  └──────────┘     └────────────────┘   │
│         │                  │            │
│         │                  ↓            │
│         │         ┌────────────────┐   │
│         └────────→│ Strava Sync    │   │
│                   │ Service        │   │
│                   └────────────────┘   │
│                          │              │
│                          ↓              │
│                   ┌────────────────┐   │
│                   │ Strava API     │   │
│                   │ Client         │   │
│                   └────────────────┘   │
└─────────────────────────┼───────────────┘
                          │
                          ↓
                  ┌───────────────┐
                  │  Strava API   │
                  └───────────────┘
```

## Database Schema

### StravaSyncQueueEntity
```kotlin
@Entity(tableName = "strava_sync_queue")
data class StravaSyncQueueEntity(
    @PrimaryKey
    val id: String,              // UUID
    val workoutId: String,       // FK to workouts table
    val status: SyncStatus,      // PENDING, SYNCING, SYNCED, FAILED
    val stravaActivityId: Long?, // Strava's activity ID (after sync)
    val queuedAt: Long,          // Timestamp when queued
    val lastAttemptAt: Long?,    // Last sync attempt timestamp
    val completedAt: Long?,      // Timestamp when synced
    val retryCount: Int = 0,     // Number of retry attempts
    val errorMessage: String?,   // Error if failed
    val isUpdate: Boolean = false // True if updating existing activity
)
```

### StravaAuthEntity
```kotlin
@Entity(tableName = "strava_auth")
data class StravaAuthEntity(
    @PrimaryKey
    val id: Int = 1,              // Always 1 (singleton)
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,          // Timestamp when token expires
    val athleteId: Long,
    val lastRefreshedAt: Long,
    val scope: String = "activity:write"
)
```

## OAuth Flow

### Authorization URL
```kotlin
fun getStravaAuthUrl(): String {
    val clientId = BuildConfig.STRAVA_CLIENT_ID
    val redirectUri = "fortisl upus://strava/callback"
    val scope = "activity:write"

    return "https://www.strava.com/oauth/authorize" +
           "?client_id=$clientId" +
           "&redirect_uri=$redirectUri" +
           "&response_type=code" +
           "&scope=$scope"
}
```

### Token Exchange
```kotlin
suspend fun exchangeCodeForToken(code: String): StravaTokenResponse {
    val response = stravaApi.exchangeToken(
        clientId = BuildConfig.STRAVA_CLIENT_ID,
        clientSecret = BuildConfig.STRAVA_CLIENT_SECRET,
        code = code,
        grantType = "authorization_code"
    )

    // Save tokens to database
    stravaAuthDao.insertOrUpdate(
        StravaAuthEntity(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000),
            athleteId = response.athlete.id,
            lastRefreshedAt = System.currentTimeMillis()
        )
    )

    return response
}
```

### Token Refresh
```kotlin
suspend fun refreshAccessToken(): String {
    val auth = stravaAuthDao.getAuth() ?: throw UnauthorizedException()

    if (System.currentTimeMillis() < auth.expiresAt) {
        return auth.accessToken  // Token still valid
    }

    // Refresh expired token
    val response = stravaApi.refreshToken(
        clientId = BuildConfig.STRAVA_CLIENT_ID,
        clientSecret = BuildConfig.STRAVA_CLIENT_SECRET,
        refreshToken = auth.refreshToken,
        grantType = "refresh_token"
    )

    // Update stored tokens
    stravaAuthDao.insertOrUpdate(
        auth.copy(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000),
            lastRefreshedAt = System.currentTimeMillis()
        )
    )

    return response.accessToken
}
```

## Activity Formatting

### Workout to Strava Mapper
```kotlin
object WorkoutToStravaMapper {
    fun mapToActivityRequest(
        workout: Workout,
        startTime: Long,  // Milliseconds since epoch
        endTime: Long     // Milliseconds since epoch
    ): StravaActivityRequest {
        return StravaActivityRequest(
            name = "${workout.type.displayName} Workout",
            type = "WeightTraining",
            sportType = "WeightTraining",
            startDateLocal = formatIso8601(startTime),
            elapsedTime = ((endTime - startTime) / 1000).toInt(), // Seconds
            description = StravaDescriptionFormatter.format(workout),
            trainer = false,
            commute = false
        )
    }

    private fun formatIso8601(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        return instant.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
```

### Description Formatter (Format A - Detailed with Emojis)
```kotlin
object StravaDescriptionFormatter {
    fun format(workout: Workout): String {
        val sections = mutableListOf<String>()

        // Header with workout type
        sections.add("💪 ${workout.type.displayName.uppercase()} Workout\n")

        // Group exercises by muscle group
        val exercisesByMuscle = workout.exercises.groupBy {
            it.exercise.muscleGroups.firstOrNull() ?: MuscleGroup.CORE
        }

        // Format each muscle group section
        exercisesByMuscle.forEach { (muscleGroup, exercises) ->
            sections.add("${muscleGroup.displayName}:")
            exercises.forEach { workoutExercise ->
                val setsSummary = formatSetsSummary(workoutExercise.sets)
                sections.add("• ${workoutExercise.exercise.name}: $setsSummary")
            }
            sections.add("")  // Blank line between groups
        }

        // Footer with total volume and duration
        sections.add("━━━━━━━━━━━━━━━━━━")
        sections.add("📊 Total Volume: ${formatVolume(workout.totalVolume)}")
        sections.add("⏱️ Duration: ${workout.durationMinutes} minutes")

        return sections.joinToString("\n")
    }

    private fun formatSetsSummary(sets: List<Set>): String {
        val completedSets = sets.filter { it.completed }
        if (completedSets.isEmpty()) return "0 sets"

        // Group sets by weight
        val setsByWeight = completedSets.groupBy { it.weight }

        return setsByWeight.entries.joinToString(", ") { (weight, setsAtWeight) ->
            val setCount = setsAtWeight.size
            val reps = setsAtWeight.first().reps
            "$setCount×$reps @ ${weight.toInt()} lbs"
        }
    }

    private fun formatVolume(volume: Double): String {
        return "${volume.toInt().toString().replace(
            Regex("(\\d)(?=(\\d{3})+$)"),
            "$1,"
        )} lbs"
    }
}
```

**Example Output**:
```
💪 PUSH WORKOUT

Chest:
• Barbell Bench Press: 3×10 @ 135 lbs
• Incline Dumbbell Press: 3×12 @ 50 lbs

Shoulder:
• Overhead Press: 4×8 @ 95 lbs

Tricep:
• Tricep Dips: 3×12 @ 0 lbs

━━━━━━━━━━━━━━━━━━
📊 Total Volume: 12,450 lbs
⏱️ Duration: 58 minutes
```

## Sync Queue Management

### Create Queue Entry
```kotlin
suspend fun queueWorkoutForSync(workoutId: String) {
    val queueEntry = StravaSyncQueueEntity(
        id = UUID.randomUUID().toString(),
        workoutId = workoutId,
        status = SyncStatus.PENDING,
        stravaActivityId = null,
        queuedAt = System.currentTimeMillis(),
        lastAttemptAt = null,
        completedAt = null,
        retryCount = 0,
        errorMessage = null,
        isUpdate = false
    )

    stravaSyncDao.insert(queueEntry)

    // Trigger background worker
    enqueueSyncWorker()
}
```

### Sync Worker (WorkManager)
```kotlin
class StravaSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendingItems = stravaSyncDao.getPendingItems()

        pendingItems.forEach { queueItem ->
            try {
                // Update status to SYNCING
                stravaSyncDao.updateStatus(queueItem.id, SyncStatus.SYNCING)

                // Get fresh access token
                val accessToken = refreshAccessToken()

                // Get workout data
                val workout = workoutRepository.getWorkoutById(queueItem.workoutId)
                    ?: throw WorkoutNotFoundException()

                // Map to Strava format
                val activityRequest = WorkoutToStravaMapper.mapToActivityRequest(
                    workout,
                    workout.startTime!!,
                    workout.endTime!!
                )

                // Send to Strava
                val response = stravaApi.createActivity(
                    authorization = "Bearer $accessToken",
                    activity = activityRequest
                )

                // Mark as synced
                stravaSyncDao.update(
                    queueItem.copy(
                        status = SyncStatus.SYNCED,
                        stravaActivityId = response.id,
                        completedAt = System.currentTimeMillis()
                    )
                )

                // Show success notification
                showNotification("Workout synced to Strava!")

            } catch (e: Exception) {
                // Handle failure
                handleSyncFailure(queueItem, e)
            }
        }

        return Result.success()
    }

    private suspend fun handleSyncFailure(
        queueItem: StravaSyncQueueEntity,
        error: Exception
    ) {
        val newRetryCount = queueItem.retryCount + 1

        if (newRetryCount >= MAX_RETRY_COUNT) {
            // Give up after max retries
            stravaSyncDao.update(
                queueItem.copy(
                    status = SyncStatus.FAILED,
                    errorMessage = error.message,
                    lastAttemptAt = System.currentTimeMillis(),
                    retryCount = newRetryCount
                )
            )
            showNotification("Failed to sync workout to Strava")
        } else {
            // Retry later
            stravaSyncDao.update(
                queueItem.copy(
                    status = SyncStatus.PENDING,
                    errorMessage = error.message,
                    lastAttemptAt = System.currentTimeMillis(),
                    retryCount = newRetryCount
                )
            )
        }
    }

    companion object {
        const val MAX_RETRY_COUNT = 3
    }
}
```

## Error Handling

### Common Errors
- **401 Unauthorized**: Token expired or invalid → Refresh token
- **403 Forbidden**: Insufficient scope → Re-authenticate
- **429 Rate Limited**: Too many requests → Backoff and retry
- **500 Server Error**: Strava issue → Retry with exponential backoff

### Retry Strategy
```kotlin
fun calculateBackoffDelay(retryCount: Int): Long {
    return (2.0.pow(retryCount) * 1000).toLong()  // Exponential backoff
    // Retry 1: 2 seconds
    // Retry 2: 4 seconds
    // Retry 3: 8 seconds
}
```

## UI Status Indicators

### Sync Status Badge
```kotlin
@Composable
fun SyncStatusBadge(status: SyncStatus) {
    when (status) {
        SyncStatus.PENDING -> Icon(Icons.Default.Schedule, tint = Color.Gray)
        SyncStatus.SYNCING -> CircularProgressIndicator(modifier = Modifier.size(16.dp))
        SyncStatus.SYNCED -> Icon(Icons.Default.CheckCircle, tint = Color.Green)
        SyncStatus.FAILED -> Icon(Icons.Default.Error, tint = Color.Red)
    }
}
```

## Privacy Considerations

- Uses Strava user's default privacy settings
- No custom privacy control in app
- Activities visible to Strava followers per user's settings
- User can delete activities directly in Strava app

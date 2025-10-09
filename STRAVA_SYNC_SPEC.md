# Strava Workout Sync - Technical Specification

> **Based on requirements gathered from STRAVA_SYNC_REQUIREMENTS.md**

---

## 📊 Requirements Summary

### **Key Decisions**
- ✅ **Auto-sync** immediately when workout marked complete
- ✅ **Weight Training** activity type
- ✅ **Format A** (Detailed with emojis) for description
- ✅ **Auto-redirect** to Strava OAuth if not connected
- ✅ **Queue offline** workouts for later sync
- ✅ **Ask user** before updating edited workouts
- ✅ **3-4 workouts/week** volume (low-medium)
- ✅ **Manual time tracking** (already in place)

### **Data to Sync**
- Exercise names and sets/reps
- Workout duration (start/end time)
- Muscle groups worked
- Workout type (PUSH/PULL)
- Total volume (sets × reps × weight)

### **Not Syncing**
- Calories burned
- Heart rate data
- Custom notes
- Historical workouts (future workouts only)

---

## 🏗️ System Architecture

### **Components**

```
┌─────────────────────────────────────────────────────────┐
│                      Workout App                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────┐      ┌──────────────────────────┐   │
│  │   Workout    │─────→│   Strava Sync Queue      │   │
│  │  Completion  │      │   (Room Database)        │   │
│  └──────────────┘      └──────────────────────────┘   │
│         │                        │                      │
│         │                        ↓                      │
│         │              ┌──────────────────────────┐    │
│         └─────────────→│  Strava Sync Service     │    │
│                        │  (Background Worker)     │    │
│                        └──────────────────────────┘    │
│                                 │                       │
│                                 ↓                       │
│                        ┌──────────────────────────┐    │
│                        │   Strava API Client      │    │
│                        │   (Retrofit + OAuth)     │    │
│                        └──────────────────────────┘    │
│                                 │                       │
└─────────────────────────────────┼───────────────────────┘
                                  │
                                  ↓
                        ┌──────────────────────────┐
                        │      Strava API          │
                        │  (activities endpoint)   │
                        └──────────────────────────┘
```

### **Data Flow**

1. **Workout Completion** → User marks workout as complete
2. **Queue Creation** → Create `StravaSyncQueue` entry with `PENDING` status
3. **Background Worker** → WorkManager picks up pending sync
4. **Auth Check** → Verify Strava token, refresh if needed
5. **Data Transform** → Convert workout to Strava activity format
6. **API Call** → POST to Strava `/activities` endpoint
7. **Status Update** → Mark as `SYNCED` or `FAILED`
8. **Notification** → Show success/failure notification

---

## 💾 Database Schema

### **New Tables**

#### **strava_sync_queue**
```kotlin
@Entity(tableName = "strava_sync_queue")
data class StravaSyncQueueEntity(
    @PrimaryKey
    val id: String,              // UUID
    val workoutId: String,       // FK to workouts table
    val status: SyncStatus,      // PENDING, SYNCING, SYNCED, FAILED
    val stravaActivityId: Long?, // Strava's activity ID (after sync)
    val createdAt: Long,         // Timestamp when queued
    val syncedAt: Long?,         // Timestamp when synced
    val errorMessage: String?,   // Error if failed
    val retryCount: Int = 0,     // Number of retry attempts
    val lastAttemptAt: Long?     // Last sync attempt timestamp
)

enum class SyncStatus {
    PENDING,    // Waiting to sync
    SYNCING,    // Currently syncing
    SYNCED,     // Successfully synced
    FAILED      // Sync failed (with retry limit reached)
}
```

#### **strava_auth**
```kotlin
@Entity(tableName = "strava_auth")
data class StravaAuthEntity(
    @PrimaryKey
    val id: Int = 1,             // Singleton (only one row)
    val accessToken: String?,
    val refreshToken: String?,
    val expiresAt: Long?,        // Token expiration timestamp
    val athleteId: Long?,        // Strava athlete ID
    val athleteName: String?     // Athlete name for display
)
```

---

## 🔄 Data Mapping

### **Workout → Strava Activity**

#### **Domain Model**
```kotlin
data class Workout(
    val id: String,
    val name: String,
    val type: WorkoutType,      // PUSH, PULL
    val status: WorkoutStatus,  // ACTIVE, COMPLETED
    val date: LocalDateTime,
    val exercises: List<WorkoutExercise>,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?
)

data class WorkoutExercise(
    val exerciseId: String,
    val name: String,
    val muscleGroups: List<MuscleGroup>,
    val sets: List<Set>
)

data class Set(
    val reps: Int,
    val weight: Double,
    val restTime: Int
)
```

#### **Strava API Format**
```kotlin
data class StravaActivity(
    val name: String,                // "💪 PUSH Workout"
    val type: String,                // "WeightTraining"
    val sport_type: String,          // "WeightTraining"
    val start_date_local: String,    // ISO 8601: "2025-10-08T08:30:00Z"
    val elapsed_time: Int,           // Duration in seconds
    val description: String,         // Formatted workout details
    val trainer: Boolean = false,    // Indoor workout
    val commute: Boolean = false
)
```

### **Description Format (Option A)**

```kotlin
fun formatWorkoutDescription(workout: Workout): String {
    val emoji = when (workout.type) {
        WorkoutType.PUSH -> "💪"
        WorkoutType.PULL -> "🏋️"
    }

    val header = "$emoji ${workout.type} Workout\n\n"

    // Group exercises by muscle group
    val exercisesByMuscle = workout.exercises.groupBy {
        it.muscleGroups.firstOrNull() ?: MuscleGroup.CORE
    }

    val exerciseDetails = exercisesByMuscle.map { (muscle, exercises) ->
        val muscleEmoji = when (muscle) {
            MuscleGroup.CHEST -> "🦴"
            MuscleGroup.BACK -> "🔙"
            MuscleGroup.SHOULDER -> "💪"
            MuscleGroup.LEGS -> "🦵"
            MuscleGroup.BICEP -> "💪"
            MuscleGroup.TRICEP -> "💪"
            MuscleGroup.CORE -> "🎯"
        }

        val muscleSection = "$muscleEmoji ${muscle.name}:\n"
        val exerciseLines = exercises.map { exercise ->
            val setsSummary = summarizeSets(exercise.sets)
            "• ${exercise.name}: $setsSummary"
        }.joinToString("\n")

        "$muscleSection$exerciseLines"
    }.joinToString("\n\n")

    val totalVolume = calculateTotalVolume(workout)
    val duration = calculateDuration(workout)

    val footer = "\n\nTotal Volume: ${formatVolume(totalVolume)}\nDuration: $duration minutes"

    return header + exerciseDetails + footer
}

// Helper: "3×10 @ 135 lbs" or "4×8-12 @ 50 lbs" (if reps vary)
fun summarizeSets(sets: List<Set>): String {
    val uniqueReps = sets.map { it.reps }.distinct()
    val uniqueWeights = sets.map { it.weight }.distinct()

    val repsStr = if (uniqueReps.size == 1) {
        uniqueReps.first().toString()
    } else {
        "${uniqueReps.min()}-${uniqueReps.max()}"
    }

    val weightStr = if (uniqueWeights.size == 1) {
        uniqueWeights.first()
    } else {
        uniqueWeights.average()
    }

    return "${sets.size}×$repsStr @ $weightStr lbs"
}
```

**Example Output:**
```
💪 PUSH Workout

🦴 Chest:
• Barbell Bench Press: 3×10 @ 135 lbs
• Incline Dumbbell Press: 3×12 @ 50 lbs

💪 Shoulder:
• Dumbbell Shoulder Press: 3×10 @ 40 lbs
• Lateral Raises: 3×15 @ 20 lbs

🦵 Legs:
• Squats: 4×8 @ 185 lbs
• Leg Press: 3×12 @ 270 lbs

Total Volume: 15,780 lbs
Duration: 62 minutes
```

---

## 🔐 Authentication Flow

### **Strava OAuth 2.0**

#### **1. Initial Connection (Settings Screen)**

```kotlin
// User clicks "Connect Strava" in settings
fun connectStrava() {
    val authUrl = buildStravaAuthUrl(
        clientId = STRAVA_CLIENT_ID,
        redirectUri = "workoutapp://strava-oauth",
        scope = "activity:write"
    )

    // Open browser for OAuth
    openBrowser(authUrl)
}

// Deep link handler
fun handleStravaCallback(code: String) {
    viewModelScope.launch {
        val tokens = stravaAuthRepository.exchangeCodeForTokens(code)

        if (tokens != null) {
            // Save tokens to database
            saveStravaAuth(tokens)
            showSuccessMessage("Connected to Strava!")
        } else {
            showErrorMessage("Failed to connect")
        }
    }
}
```

#### **2. Token Refresh**

```kotlin
suspend fun getValidAccessToken(): String? {
    val auth = stravaAuthDao.getAuth()

    if (auth == null || auth.accessToken == null) {
        return null
    }

    // Check if token is expired
    if (auth.expiresAt!! < System.currentTimeMillis()) {
        // Refresh token
        val newTokens = stravaApi.refreshToken(
            clientId = STRAVA_CLIENT_ID,
            clientSecret = STRAVA_CLIENT_SECRET,
            refreshToken = auth.refreshToken!!
        )

        // Update database
        stravaAuthDao.updateTokens(newTokens)
        return newTokens.accessToken
    }

    return auth.accessToken
}
```

---

## ⚙️ Sync Service Implementation

### **WorkManager Background Worker**

```kotlin
class StravaSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendingSyncs = syncQueueDao.getPendingSyncs()

        if (pendingSyncs.isEmpty()) {
            return Result.success()
        }

        val accessToken = getValidAccessToken()

        if (accessToken == null) {
            // Not authenticated, skip
            return Result.success()
        }

        var successCount = 0
        var failCount = 0

        for (queueItem in pendingSyncs) {
            try {
                // Update status to SYNCING
                syncQueueDao.updateStatus(queueItem.id, SyncStatus.SYNCING)

                // Get workout data
                val workout = workoutDao.getWorkoutById(queueItem.workoutId)

                if (workout == null) {
                    // Workout deleted, remove from queue
                    syncQueueDao.delete(queueItem.id)
                    continue
                }

                // Convert to Strava format
                val activity = mapWorkoutToStravaActivity(workout)

                // Sync to Strava
                val response = stravaApi.createActivity(
                    accessToken = "Bearer $accessToken",
                    activity = activity
                )

                if (response.isSuccessful) {
                    val stravaActivityId = response.body()?.id

                    // Update queue as SYNCED
                    syncQueueDao.markAsSynced(
                        id = queueItem.id,
                        stravaActivityId = stravaActivityId,
                        syncedAt = System.currentTimeMillis()
                    )

                    successCount++

                    // Show notification
                    showSyncSuccessNotification(workout.name)
                } else {
                    handleSyncFailure(queueItem, response.errorBody()?.string())
                    failCount++
                }

            } catch (e: Exception) {
                handleSyncFailure(queueItem, e.message)
                failCount++
            }
        }

        return if (failCount == 0) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    private suspend fun handleSyncFailure(
        queueItem: StravaSyncQueueEntity,
        errorMessage: String?
    ) {
        val newRetryCount = queueItem.retryCount + 1

        if (newRetryCount >= MAX_RETRY_ATTEMPTS) {
            // Give up, mark as FAILED
            syncQueueDao.markAsFailed(
                id = queueItem.id,
                errorMessage = errorMessage ?: "Unknown error"
            )

            // Show failure notification with retry button
            showSyncFailureNotification(queueItem.workoutId, errorMessage)
        } else {
            // Update retry count
            syncQueueDao.updateRetryCount(
                id = queueItem.id,
                retryCount = newRetryCount,
                lastAttemptAt = System.currentTimeMillis()
            )
        }
    }

    companion object {
        const val MAX_RETRY_ATTEMPTS = 3
        const val WORK_NAME = "strava_sync"
    }
}
```

### **Triggering Sync on Workout Completion**

```kotlin
// In WorkoutViewModel or Repository
suspend fun markWorkoutAsComplete(workoutId: String) {
    // Update workout status
    workoutDao.updateStatus(workoutId, WorkoutStatus.COMPLETED)

    // Check if Strava sync is enabled
    val stravaEnabled = settingsRepository.isStravaSyncEnabled()
    val hasStravaAuth = stravaAuthDao.getAuth() != null

    if (stravaEnabled && hasStravaAuth) {
        // Queue for sync
        val queueItem = StravaSyncQueueEntity(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            status = SyncStatus.PENDING,
            stravaActivityId = null,
            createdAt = System.currentTimeMillis(),
            syncedAt = null,
            errorMessage = null,
            retryCount = 0,
            lastAttemptAt = null
        )

        syncQueueDao.insert(queueItem)

        // Trigger WorkManager
        scheduleImmediateSync()
    }
}

fun scheduleImmediateSync() {
    val syncRequest = OneTimeWorkRequestBuilder<StravaSyncWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork(
            StravaSyncWorker.WORK_NAME,
            ExistingWorkPolicy.APPEND,
            syncRequest
        )
}
```

---

## 🎨 UI Components

### **Settings Screen**

```kotlin
@Composable
fun StravaSettingsSection(
    viewModel: SettingsViewModel
) {
    val stravaAuth by viewModel.stravaAuth.collectAsState()
    val syncEnabled by viewModel.stravaSyncEnabled.collectAsState()

    Column {
        Text(
            text = "Strava Integration",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (stravaAuth != null) {
            // Connected
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Connected as ${stravaAuth?.athleteName}")
                    Text(
                        text = "Athlete ID: ${stravaAuth?.athleteId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = { viewModel.disconnectStrava() }) {
                    Text("Disconnect")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auto-sync toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-sync workouts")

                Switch(
                    checked = syncEnabled,
                    onCheckedChange = { viewModel.setStravaSyncEnabled(it) }
                )
            }

        } else {
            // Not connected
            Button(
                onClick = { viewModel.connectStrava() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_strava),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect Strava")
            }
        }
    }
}
```

### **Workout Detail Screen - Sync Status Badge**

```kotlin
@Composable
fun WorkoutSyncStatus(
    workout: Workout,
    syncStatus: StravaSyncQueueEntity?
) {
    when (syncStatus?.status) {
        SyncStatus.PENDING -> {
            Chip(
                onClick = { },
                label = { Text("Syncing to Strava...") },
                leadingIcon = { CircularProgressIndicator(modifier = Modifier.size(16.dp)) }
            )
        }

        SyncStatus.SYNCED -> {
            Chip(
                onClick = { /* Open Strava activity */ },
                label = { Text("Synced to Strava") },
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                colors = ChipDefaults.chipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        SyncStatus.FAILED -> {
            Chip(
                onClick = { /* Retry sync */ },
                label = { Text("Sync Failed - Tap to Retry") },
                leadingIcon = { Icon(Icons.Default.Error, contentDescription = null) },
                colors = ChipDefaults.chipColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }

        else -> {
            // No sync status
        }
    }
}
```

---

## 🔔 Notifications

### **Success Notification**

```kotlin
fun showSyncSuccessNotification(workoutName: String) {
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_strava)
        .setContentTitle("Synced to Strava")
        .setContentText("$workoutName synced successfully")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(SYNC_SUCCESS_ID, notification)
}
```

### **Failure Notification with Retry**

```kotlin
fun showSyncFailureNotification(workoutId: String, errorMessage: String?) {
    val retryIntent = Intent(context, StravaSyncReceiver::class.java).apply {
        action = ACTION_RETRY_SYNC
        putExtra(EXTRA_WORKOUT_ID, workoutId)
    }

    val retryPendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        retryIntent,
        PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_error)
        .setContentTitle("Strava Sync Failed")
        .setContentText(errorMessage ?: "Failed to sync workout")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .addAction(
            R.drawable.ic_refresh,
            "Retry",
            retryPendingIntent
        )
        .setAutoCancel(true)
        .build()

    notificationManager.notify(SYNC_FAILURE_ID, notification)
}
```

---

## 📝 Strava API Integration

### **API Client (Retrofit)**

```kotlin
interface StravaApi {

    @POST("oauth/token")
    suspend fun exchangeCodeForTokens(
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String,
        @Query("code") code: String,
        @Query("grant_type") grantType: String = "authorization_code"
    ): Response<StravaTokenResponse>

    @POST("oauth/token")
    suspend fun refreshToken(
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String,
        @Query("refresh_token") refreshToken: String,
        @Query("grant_type") grantType: String = "refresh_token"
    ): StravaTokenResponse

    @POST("api/v3/activities")
    suspend fun createActivity(
        @Header("Authorization") accessToken: String,
        @Body activity: StravaActivity
    ): Response<StravaActivityResponse>

    @PUT("api/v3/activities/{id}")
    suspend fun updateActivity(
        @Header("Authorization") accessToken: String,
        @Path("id") activityId: Long,
        @Body activity: StravaActivity
    ): Response<StravaActivityResponse>

    @GET("api/v3/athlete")
    suspend fun getAthlete(
        @Header("Authorization") accessToken: String
    ): Response<StravaAthlete>
}

data class StravaTokenResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_at: Long,
    val athlete: StravaAthlete
)

data class StravaAthlete(
    val id: Long,
    val username: String?,
    val firstname: String?,
    val lastname: String?
)

data class StravaActivityResponse(
    val id: Long,
    val name: String,
    val start_date: String,
    val type: String
)
```

### **Base URL**
```
https://www.strava.com/
```

### **Required Scopes**
```
activity:write
```

---

## 🚦 Implementation Phases

### **Phase 1: MVP (Core Functionality)**

**Goal:** Basic auto-sync with minimal UI

**Tasks:**
1. ✅ Setup Strava OAuth flow
   - Register app on Strava
   - Implement deep link handling
   - Build auth screens

2. ✅ Database schema
   - Create `strava_sync_queue` table
   - Create `strava_auth` table
   - Add DAOs

3. ✅ Sync service
   - Build WorkManager worker
   - Implement data mapping
   - Add retry logic

4. ✅ Settings integration
   - Add "Connect Strava" button
   - Add auto-sync toggle
   - Show connection status

5. ✅ Basic notifications
   - Success notification
   - Failure notification with retry

**Deliverables:**
- ✅ Workouts auto-sync to Strava on completion
- ✅ OAuth authentication working
- ✅ Basic error handling
- ✅ Settings screen integration

**Estimated Time:** 2-3 weeks

---

### **Phase 2: Enhanced UX**

**Goal:** Better user feedback and control

**Tasks:**
1. ✅ Sync status badges
   - Show sync status on workout cards
   - Add "Syncing", "Synced", "Failed" indicators

2. ✅ Workout editing flow
   - Detect edited workouts
   - Ask user if they want to update Strava
   - Handle update API calls

3. ✅ Manual retry
   - Add retry button on failed syncs
   - Bulk retry option in settings

4. ✅ Improved error messages
   - Better error categorization
   - Actionable error messages

**Deliverables:**
- ✅ Transparent sync status
- ✅ User control over updates
- ✅ Better error experience

**Estimated Time:** 1-2 weeks

---

### **Phase 3: Additional Features**

**Goal:** Nice-to-have enhancements

**Tasks:**
1. ✅ Workout summary image generation
   - Create shareable workout images
   - Export to gallery or share directly

2. ✅ Better offline handling
   - Show pending sync count
   - Manual sync trigger

3. ✅ Sync history
   - View all synced workouts
   - Re-sync option
   - View on Strava link

**Deliverables:**
- ✅ Shareable workout images
- ✅ Sync history screen
- ✅ Better offline UX

**Estimated Time:** 1-2 weeks

---

## 📦 Dependencies

### **New Gradle Dependencies**

```kotlin
// Retrofit for API calls
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// WorkManager for background sync
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Deep linking
// (Already included in Android)
```

### **Strava App Registration**

1. Go to https://www.strava.com/settings/api
2. Register new application
3. Get `Client ID` and `Client Secret`
4. Set callback URL: `workoutapp://strava-oauth`

---

## 🧪 Testing Strategy

### **Unit Tests**

- Data mapping (Workout → StravaActivity)
- Description formatting
- Volume calculations
- Token refresh logic

### **Integration Tests**

- OAuth flow end-to-end
- Sync worker execution
- Queue management
- Retry logic

### **Manual Testing Checklist**

- [ ] Connect Strava account
- [ ] Complete workout → verify auto-sync
- [ ] Check Strava app for activity
- [ ] Edit workout → verify update prompt
- [ ] Disconnect Strava
- [ ] Test offline queueing
- [ ] Test retry on failure
- [ ] Test with 3-4 workouts/week volume

---

## 🔒 Security Considerations

### **Token Storage**

- ✅ Store tokens in encrypted Room database
- ✅ Use Android Keystore for additional encryption
- ✅ Never log tokens

### **API Security**

- ✅ Use HTTPS only
- ✅ Validate SSL certificates
- ✅ Implement certificate pinning (optional)

### **User Privacy**

- ✅ Respect Strava's default privacy settings
- ✅ Clear explanation of what data is synced
- ✅ Easy disconnect/delete option

---

## 📊 Success Metrics

### **Phase 1 (MVP)**
- ✅ 100% of completed workouts sync successfully
- ✅ < 5% sync failure rate
- ✅ OAuth flow completion > 90%

### **Phase 2 (Enhanced)**
- ✅ < 2% sync failure rate
- ✅ 90% user satisfaction with sync transparency
- ✅ < 1 support ticket per 100 syncs

---

## 📚 References

- **Strava API Docs:** https://developers.strava.com/docs/reference/
- **OAuth Guide:** https://developers.strava.com/docs/authentication/
- **Activity Types:** https://developers.strava.com/docs/reference/#api-models-ActivityType
- **Rate Limits:** 600 requests per 15 minutes, 30,000 per day

---

## ✅ Next Steps

1. **Review this specification** - Make sure it aligns with your vision
2. **Get Strava API credentials** - Register app on Strava
3. **Start Phase 1 implementation** - Begin with OAuth flow
4. **Iterate based on feedback** - Adjust as needed

Ready to start building? 🚀

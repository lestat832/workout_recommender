# Strava Sync - Implementation Roadmap

> **Quick reference guide for implementing Strava workout sync**

---

## 🎯 Implementation Overview

**Total Estimated Time:** 4-7 weeks
- **Phase 1 (MVP):** 2-3 weeks
- **Phase 2 (Enhanced UX):** 1-2 weeks
- **Phase 3 (Additional Features):** 1-2 weeks

---

## 📋 Pre-Implementation Checklist

### **1. Get Strava API Credentials**

- [ ] Go to https://www.strava.com/settings/api
- [ ] Click "Create an App"
- [ ] Fill in application details:
  - **Application Name:** Workout App
  - **Category:** Training
  - **Club:** (leave blank)
  - **Website:** Your app website or GitHub
  - **Authorization Callback Domain:** `workoutapp://strava-oauth`
- [ ] Save **Client ID** and **Client Secret**
- [ ] Store in `local.properties`:
  ```properties
  STRAVA_CLIENT_ID=your_client_id
  STRAVA_CLIENT_SECRET=your_client_secret
  ```

### **2. Update Gradle**

Add to `app/build.gradle.kts`:

```kotlin
// In android block
buildFeatures {
    buildConfig = true
}

// In dependencies
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("androidx.work:work-runtime-ktx:2.9.0")
```

### **3. Update AndroidManifest.xml**

Add deep link handling:

```xml
<activity android:name=".presentation.MainActivity">
    <!-- Existing intent filters -->

    <!-- Strava OAuth callback -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="workoutapp"
            android:host="strava-oauth" />
    </intent-filter>
</activity>
```

---

## 🏗️ Phase 1: MVP Implementation

### **Week 1: Database & Models**

#### **Day 1-2: Database Schema**

**Files to Create:**
- `data/database/entities/StravaSyncQueueEntity.kt`
- `data/database/entities/StravaAuthEntity.kt`
- `data/database/dao/StravaSyncDao.kt`
- `data/database/dao/StravaAuthDao.kt`

**Tasks:**
```kotlin
// 1. Create StravaSyncQueueEntity
@Entity(tableName = "strava_sync_queue")
data class StravaSyncQueueEntity(...)

// 2. Create StravaAuthEntity
@Entity(tableName = "strava_auth")
data class StravaAuthEntity(...)

// 3. Add DAOs with suspend functions
@Dao
interface StravaSyncDao {
    @Query("SELECT * FROM strava_sync_queue WHERE status = 'PENDING'")
    suspend fun getPendingSyncs(): List<StravaSyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StravaSyncQueueEntity)

    // ... more methods
}

// 4. Update WorkoutDatabase version
@Database(
    entities = [..., StravaSyncQueueEntity::class, StravaAuthEntity::class],
    version = 6  // Increment version
)

// 5. Add migration from version 5 to 6
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create strava_sync_queue table
        database.execSQL("""
            CREATE TABLE strava_sync_queue (
                id TEXT NOT NULL PRIMARY KEY,
                workoutId TEXT NOT NULL,
                status TEXT NOT NULL,
                stravaActivityId INTEGER,
                createdAt INTEGER NOT NULL,
                syncedAt INTEGER,
                errorMessage TEXT,
                retryCount INTEGER NOT NULL DEFAULT 0,
                lastAttemptAt INTEGER
            )
        """)

        // Create strava_auth table
        database.execSQL("""
            CREATE TABLE strava_auth (
                id INTEGER NOT NULL PRIMARY KEY,
                accessToken TEXT,
                refreshToken TEXT,
                expiresAt INTEGER,
                athleteId INTEGER,
                athleteName TEXT
            )
        """)
    }
}
```

**Test:**
- [ ] Build project successfully
- [ ] Verify tables created (Device File Explorer)

---

#### **Day 3-4: Strava API Client**

**Files to Create:**
- `data/remote/strava/StravaApi.kt`
- `data/remote/strava/StravaModels.kt`
- `data/remote/strava/StravaApiClient.kt`

**Tasks:**
```kotlin
// 1. Create API interface
interface StravaApi {
    @POST("oauth/token")
    suspend fun exchangeCodeForTokens(...): Response<StravaTokenResponse>

    @POST("api/v3/activities")
    suspend fun createActivity(...): Response<StravaActivityResponse>
}

// 2. Create data models
data class StravaActivity(
    val name: String,
    val type: String,
    val start_date_local: String,
    val elapsed_time: Int,
    val description: String
)

// 3. Build Retrofit client
object StravaApiClient {
    private const val BASE_URL = "https://www.strava.com/"

    val api: StravaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(StravaApi::class.java)
    }
}
```

**Test:**
- [ ] Mock API call compiles
- [ ] Verify network requests work (use Postman first)

---

#### **Day 5-7: OAuth Flow**

**Files to Create:**
- `presentation/settings/StravaAuthScreen.kt`
- `presentation/viewmodel/StravaAuthViewModel.kt`
- `domain/usecase/ConnectStravaUseCase.kt`
- `data/repository/StravaAuthRepository.kt`

**Tasks:**
```kotlin
// 1. Build auth URL
fun buildStravaAuthUrl(): String {
    return "https://www.strava.com/oauth/authorize?" +
        "client_id=$STRAVA_CLIENT_ID&" +
        "redirect_uri=workoutapp://strava-oauth&" +
        "response_type=code&" +
        "scope=activity:write"
}

// 2. Handle deep link
@Composable
fun MainActivity() {
    val navController = rememberNavController()

    // Handle Strava callback
    LaunchedEffect(Unit) {
        val uri = intent.data
        if (uri?.scheme == "workoutapp" && uri.host == "strava-oauth") {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                viewModel.handleStravaCallback(code)
            }
        }
    }
}

// 3. Exchange code for tokens
suspend fun exchangeCodeForTokens(code: String) {
    val response = stravaApi.exchangeCodeForTokens(
        clientId = STRAVA_CLIENT_ID,
        clientSecret = STRAVA_CLIENT_SECRET,
        code = code
    )

    if (response.isSuccessful) {
        val tokens = response.body()!!
        saveTokens(tokens)
    }
}
```

**Test:**
- [ ] Click "Connect Strava" → Opens browser
- [ ] Authorize on Strava → Redirects back to app
- [ ] Tokens saved in database
- [ ] Shows "Connected as [Name]"

---

### **Week 2: Data Mapping & Formatting**

#### **Day 1-3: Workout Formatter**

**Files to Create:**
- `domain/mapper/WorkoutToStravaMapper.kt`
- `domain/formatter/StravaDescriptionFormatter.kt`

**Tasks:**
```kotlin
// 1. Format workout description
fun formatWorkoutDescription(workout: Workout): String {
    val emoji = if (workout.type == WorkoutType.PUSH) "💪" else "🏋️"
    val header = "$emoji ${workout.type} Workout\n\n"

    // Group by muscle
    val byMuscle = workout.exercises.groupBy {
        it.muscleGroups.firstOrNull()
    }

    val exerciseDetails = byMuscle.map { (muscle, exercises) ->
        formatMuscleGroup(muscle, exercises)
    }.joinToString("\n\n")

    val totalVolume = calculateTotalVolume(workout)
    val duration = calculateDuration(workout)

    return header + exerciseDetails +
           "\n\nTotal Volume: ${formatVolume(totalVolume)}\n" +
           "Duration: $duration minutes"
}

// 2. Calculate totals
fun calculateTotalVolume(workout: Workout): Double {
    return workout.exercises.sumOf { exercise ->
        exercise.sets.sumOf { set ->
            set.reps * set.weight
        }
    }
}

fun calculateDuration(workout: Workout): Int {
    val start = workout.startTime ?: return 0
    val end = workout.endTime ?: return 0
    return Duration.between(start, end).toMinutes().toInt()
}
```

**Test:**
- [ ] Format sample workout
- [ ] Verify output matches "Format A"
- [ ] Test with different muscle groups
- [ ] Test edge cases (no exercises, no time)

---

#### **Day 4-5: Map to Strava Format**

**Tasks:**
```kotlin
fun mapWorkoutToStravaActivity(workout: Workout): StravaActivity {
    return StravaActivity(
        name = "💪 ${workout.type} Workout",
        type = "WeightTraining",
        sport_type = "WeightTraining",
        start_date_local = workout.date.toString(),
        elapsed_time = calculateDuration(workout) * 60, // seconds
        description = formatWorkoutDescription(workout),
        trainer = false,
        commute = false
    )
}
```

**Test:**
- [ ] Convert workout to Strava format
- [ ] Verify JSON structure
- [ ] Test with real API (create test activity)

---

### **Week 3: Sync Worker & Queue**

#### **Day 1-3: WorkManager Implementation**

**Files to Create:**
- `data/worker/StravaSyncWorker.kt`
- `domain/usecase/QueueWorkoutForSyncUseCase.kt`
- `domain/usecase/SyncPendingWorkoutsUseCase.kt`

**Tasks:**
```kotlin
// 1. Create worker
class StravaSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Get pending syncs
        val pending = syncQueueDao.getPendingSyncs()

        for (item in pending) {
            try {
                // Get workout
                val workout = workoutDao.getById(item.workoutId)

                // Map to Strava
                val activity = mapWorkoutToStravaActivity(workout)

                // Sync
                val response = stravaApi.createActivity(
                    accessToken = "Bearer ${getAccessToken()}",
                    activity = activity
                )

                if (response.isSuccessful) {
                    // Mark as synced
                    syncQueueDao.markAsSynced(item.id, response.body()!!.id)
                } else {
                    // Handle failure
                    handleFailure(item, response.errorBody()?.string())
                }
            } catch (e: Exception) {
                handleFailure(item, e.message)
            }
        }

        return Result.success()
    }
}

// 2. Queue on workout completion
suspend fun markWorkoutAsComplete(workoutId: String) {
    workoutDao.updateStatus(workoutId, WorkoutStatus.COMPLETED)

    // Queue for sync
    if (isStravaSyncEnabled()) {
        queueWorkoutForSync(workoutId)
    }
}

// 3. Schedule worker
fun scheduleSync() {
    val request = OneTimeWorkRequestBuilder<StravaSyncWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueue(request)
}
```

**Test:**
- [ ] Complete workout → queued for sync
- [ ] Worker runs and syncs to Strava
- [ ] Check Strava app for activity
- [ ] Verify retry on failure

---

#### **Day 4-5: Settings Integration & Notifications**

**Files to Create:**
- `presentation/settings/StravaSettingsScreen.kt`
- `utils/StravaNotifications.kt`

**Tasks:**
```kotlin
// 1. Add to settings screen
@Composable
fun StravaSettingsSection() {
    val stravaAuth by viewModel.stravaAuth.collectAsState()

    if (stravaAuth != null) {
        // Show connected state
        Text("Connected as ${stravaAuth.athleteName}")

        Switch(
            checked = syncEnabled,
            onCheckedChange = { viewModel.setSyncEnabled(it) }
        )
    } else {
        Button(onClick = { viewModel.connectStrava() }) {
            Text("Connect Strava")
        }
    }
}

// 2. Create notifications
fun showSyncSuccessNotification(workoutName: String) {
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("Synced to Strava")
        .setContentText("$workoutName synced successfully")
        .build()

    notificationManager.notify(SYNC_SUCCESS_ID, notification)
}
```

**Test:**
- [ ] Connect/disconnect Strava in settings
- [ ] Toggle auto-sync
- [ ] Verify success notification appears
- [ ] Test failure notification

---

## ✅ Phase 1 Completion Checklist

- [ ] OAuth flow working end-to-end
- [ ] Workouts auto-queue on completion
- [ ] Worker syncs to Strava successfully
- [ ] Proper error handling and retry
- [ ] Success/failure notifications
- [ ] Settings screen integration
- [ ] Manual testing with 5+ workouts

**🎉 At this point, you have a working MVP!**

---

## 🎨 Phase 2: Enhanced UX (Weeks 4-5)

### **Tasks:**

1. **Sync Status Badges**
   - Show status on workout cards
   - "Pending", "Synced", "Failed" indicators

2. **Edit Detection**
   - Detect when synced workout is edited
   - Ask user if they want to update Strava

3. **Manual Retry**
   - Retry button on failed syncs
   - Bulk retry in settings

4. **Better Error Messages**
   - Categorize errors (network, auth, server)
   - Actionable error messages

---

## 🚀 Phase 3: Additional Features (Weeks 6-7)

### **Tasks:**

1. **Workout Summary Image**
   - Generate shareable image
   - Export to gallery

2. **Sync History**
   - View all synced workouts
   - Link to Strava activity

3. **Better Offline Handling**
   - Pending sync indicator
   - Manual sync trigger

---

## 📝 Quick Commands Reference

### **Test OAuth Flow**
```
adb shell am start -W -a android.intent.action.VIEW \
  -d "workoutapp://strava-oauth?code=test_code"
```

### **Trigger Sync Worker**
```kotlin
WorkManager.getInstance(context)
    .enqueueUniqueWork(
        "strava_sync",
        ExistingWorkPolicy.REPLACE,
        syncRequest
    )
```

### **View Database**
```
Device File Explorer →
/data/data/com.workoutapp/databases/workout_database
```

---

## 🐛 Common Issues & Solutions

### **Issue: OAuth redirect not working**
**Solution:** Verify deep link in AndroidManifest.xml and test with:
```
adb shell am start -W -a android.intent.action.VIEW \
  -d "workoutapp://strava-oauth?code=test"
```

### **Issue: Worker not running**
**Solution:** Check WorkManager status:
```kotlin
WorkManager.getInstance(context)
    .getWorkInfosForUniqueWork("strava_sync")
```

### **Issue: Token expired**
**Solution:** Implement token refresh in worker before API call

---

## 📚 Resources

- **Full Spec:** `STRAVA_SYNC_SPEC.md`
- **Requirements:** `STRAVA_SYNC_REQUIREMENTS.md`
- **Strava API Docs:** https://developers.strava.com/docs/
- **WorkManager Guide:** https://developer.android.com/topic/libraries/architecture/workmanager

---

**Ready to start?** Begin with Phase 1, Week 1! 🚀

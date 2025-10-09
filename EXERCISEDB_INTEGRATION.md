# ExerciseDB Integration Guide

This document explains how to integrate the ExerciseDB (Free Exercise Database) into your workout app.

## 📊 Overview

- **Total Exercises:** 873+ exercises
- **Data Source:** [Free Exercise DB](https://github.com/yuhonas/free-exercise-db) (Public Domain)
- **API Endpoint:** `https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json`
- **No Authentication Required:** Free to use

## 🎯 What's Been Integrated

### 1. **Data Models** (`/app/src/main/java/com/workoutapp/data/remote/`)
- `ExerciseDbJson.kt` - API response model

### 2. **Mapping Logic** (`/app/src/main/java/com/workoutapp/data/mapper/`)
- `ExerciseMapper.kt` - Converts ExerciseDB format to your app's format
  - Maps 17 muscle types → 7 MuscleGroup enums
  - Classifies exercises into PUSH/PULL categories
  - Normalizes equipment names
  - Constructs full image URLs

### 3. **API Service** (`/app/src/main/java/com/workoutapp/data/remote/`)
- `ExerciseDbService.kt` - Fetches exercises from network or bundled assets
  - Primary: Fetches from GitHub
  - Fallback: Loads from `assets/exercises.json` (if bundled)

### 4. **Repository Integration** (`/app/src/main/java/com/workoutapp/data/repository/`)
- `ExerciseRepositoryImpl.kt` - Added `syncExercisesFromApi()` method
  - Fetches exercises from API
  - Transforms to entities
  - Inserts into Room database

### 5. **Initialization UseCase** (`/app/src/main/java/com/workoutapp/domain/usecase/`)
- `InitializeExercisesUseCase.kt` - Handles first-run exercise loading
  - Checks if already initialized (SharedPreferences)
  - Loads exercises on first app launch
  - Supports force refresh

## 🚀 Integration Steps

### Step 1: Add Network Permissions

Add to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Step 2: Initialize Exercises on App Startup

**Option A: In your Application class (Recommended)**

```kotlin
class WorkoutApplication : Application() {

    @Inject
    lateinit var initializeExercisesUseCase: InitializeExercisesUseCase

    override fun onCreate() {
        super.onCreate()

        // Initialize exercises in background
        CoroutineScope(Dispatchers.IO).launch {
            val result = initializeExercisesUseCase()

            if (result.isSuccess) {
                val count = result.getOrNull()
                if (count != null) {
                    Log.d("WorkoutApp", "Loaded $count exercises from ExerciseDB")
                } else {
                    Log.d("WorkoutApp", "Exercises already initialized")
                }
            } else {
                Log.e("WorkoutApp", "Failed to initialize exercises: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}
```

**Option B: In your MainActivity or splash screen**

```kotlin
class MainActivity : ComponentActivity() {

    private val initializeExercisesUseCase: InitializeExercisesUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Show loading indicator
            val result = initializeExercisesUseCase()

            // Handle result and navigate to home
            if (result.isSuccess) {
                // Exercises loaded successfully
                navigateToHome()
            } else {
                // Show error dialog
            }
        }
    }
}
```

### Step 3: (Optional) Bundle Exercises as Assets

To provide offline fallback:

1. Download exercises JSON:
   ```bash
   curl https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json -o app/src/main/assets/exercises.json
   ```

2. The `ExerciseDbService` will automatically use this if network fetch fails

### Step 4: Update Your UI

No changes needed! The exercises will automatically appear in your existing exercise selection screens since they use the same `ExerciseRepository`.

## 🔍 How It Works

### Mapping Logic

**Muscle Groups Mapping:**
```
ExerciseDB → Your App
-------------------
chest → CHEST
shoulders → SHOULDER
triceps → TRICEP
biceps → BICEP
lats, middle back, lower back, traps → BACK
quadriceps, hamstrings, glutes, calves, adductors, abductors → LEGS
abdominals → CORE
```

**PUSH/PULL Classification:**

The mapper uses two strategies:

1. **Primary (86% coverage):** Use `force` field from ExerciseDB
   - `force: "push"` → `PUSH`
   - `force: "pull"` → `PULL`
   - `force: "static"` → `PULL`

2. **Fallback:** Classify by primary muscle
   - PUSH muscles: chest, triceps, shoulders, quadriceps, calves, glutes
   - PULL muscles: biceps, lats, back muscles, hamstrings, abdominals

**Equipment Normalization:**
```
ExerciseDB → Your App
-------------------
"body only" → "Bodyweight"
"barbell" → "Barbell"
"dumbbell" → "Dumbbell"
"cable" → "Cable"
"machine" → "Machine"
"kettlebells" → "Kettlebell"
"bands" → "Resistance Band"
etc.
```

### Image URLs

Images are constructed as:
```
https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/{exercise_id}/{image_index}.jpg
```

Example:
```
https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/Barbell_Bench_Press/0.jpg
```

## 🧪 Testing

### Test Exercise Loading

```kotlin
// In a test ViewModel or debug screen
lifecycleScope.launch {
    val result = exerciseRepository.syncExercisesFromApi()

    if (result.isSuccess) {
        val count = result.getOrNull()
        println("Successfully loaded $count exercises")

        val exercises = exerciseRepository.getAllExercises().first()
        println("Total exercises in database: ${exercises.size}")

        // Test filtering
        val chestExercises = exercises.filter {
            it.muscleGroups.contains(MuscleGroup.CHEST)
        }
        println("Chest exercises: ${chestExercises.size}")
    }
}
```

### Force Refresh (for testing)

```kotlin
// Reset and reload exercises
initializeExercisesUseCase.resetInitialization()
val result = initializeExercisesUseCase.forceRefresh()
```

## 📱 User Experience Considerations

### First Launch
- **First time:** Fetches 873 exercises (~2.5 MB JSON) - takes 2-5 seconds
- **Subsequent launches:** Instant (loads from Room database)

### Loading Strategy
1. Check if exercises are initialized (SharedPreferences)
2. If not, show loading indicator
3. Fetch from network (or bundled assets)
4. Transform and insert into database
5. Mark as initialized

### Offline Support
- If bundled `exercises.json` in assets, works offline
- Otherwise, requires internet on first launch only

## 🔧 Customization

### Modify Muscle Group Mapping

Edit `ExerciseMapper.kt`:

```kotlin
private fun mapMuscleGroups(...): List<MuscleGroup> {
    // Add custom mapping logic
    when (muscle.lowercase()) {
        "chest" -> muscleGroups.add(MuscleGroup.CHEST)
        // Add your custom mappings
    }
}
```

### Change PUSH/PULL Logic

Edit `ExerciseMapper.kt`:

```kotlin
private fun mapWorkoutType(...): WorkoutType {
    // Implement your custom classification
}
```

### Filter Exercises Before Insertion

In `ExerciseRepositoryImpl.kt`:

```kotlin
suspend fun syncExercisesFromApi(): Result<Int> {
    // ... fetch exercises ...

    // Filter only exercises you want
    val filteredExercises = exercisesJson.filter { exercise ->
        exercise.equipment in listOf("barbell", "dumbbell", "body only") &&
        exercise.level == "beginner"
    }

    val entities = filteredExercises.map { it.toEntity() }
    // ... insert ...
}
```

## 🐛 Troubleshooting

### "Failed to sync exercises"
- **Check internet connection** on first launch
- **Verify** `INTERNET` permission in manifest
- **Check logs** for detailed error message
- **Fallback:** Bundle `exercises.json` in assets

### "No exercises showing in UI"
- **Verify** `InitializeExercisesUseCase` was called
- **Check** database: `adb shell "run-as com.workoutapp cat /data/data/com.workoutapp/databases/workout_database"`
- **Force refresh:** Call `initializeExercisesUseCase.forceRefresh()`

### Images not loading
- **Verify** image URLs are correct (check console logs)
- **Add** image loading library: Coil or Glide
- **Check** internet permission for image loading

## 📚 Additional Resources

- **ExerciseDB GitHub:** https://github.com/yuhonas/free-exercise-db
- **API Documentation:** See `/claudedocs/exercisedb_api_analysis.md`
- **Kotlin Implementation Reference:** See `/claudedocs/exercisedb_kotlin_implementation.kt`
- **Quick Reference:** See `/claudedocs/exercisedb_quick_reference.md`

## ✅ Next Steps

1. **Test the integration** by adding initialization to your app
2. **Monitor first launch performance** (should be 2-5 seconds)
3. **Consider bundling** `exercises.json` in assets for offline support
4. **Update UI** to handle large exercise library (add search/filtering)
5. **Add refresh mechanism** for users to update exercise database

---

**Questions or Issues?** Check the detailed documentation in `/claudedocs/` or review the implementation files.

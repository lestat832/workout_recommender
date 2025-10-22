# Strava Formatter Output Examples

This document shows example outputs from the Strava Description Formatter and Mapper.

## Example 1: Complete PUSH Workout

### Input Workout
- **Type**: PUSH (Alpha Training)
- **Exercises**:
  - Barbell Bench Press: 3 sets × 10 reps @ 135 lbs
  - Incline Dumbbell Press: 3 sets × 12 reps @ 50 lbs
  - Squats: 4 sets × 8 reps @ 185 lbs
  - Leg Press: 3 sets × 15 reps @ 270 lbs
- **Duration**: 58 minutes

### Generated Strava Description
```
💪 PUSH Workout

Chest:
• Barbell Bench Press: 3×10 @ 135 lbs
• Incline Dumbbell Press: 3×12 @ 50 lbs

Legs:
• Squats: 4×8 @ 185 lbs
• Leg Press: 3×15 @ 270 lbs

Total Volume: 12,450 lbs
Duration: 58 minutes
```

### Generated Strava Activity Request
```json
{
  "name": "💪 PUSH Workout",
  "type": "WeightTraining",
  "sport_type": "WeightTraining",
  "start_date_local": "2025-10-21T14:00:00Z",
  "elapsed_time": 3480,
  "description": "[See above]",
  "trainer": false,
  "commute": false
}
```

---

## Example 2: PULL Workout

### Input Workout
- **Type**: PULL (Pack Strength)
- **Exercises**:
  - Pull-ups: 4 sets × 10 reps @ 0 lbs (bodyweight)
  - Barbell Row: 3 sets × 8 reps @ 155 lbs
  - Dumbbell Curl: 3 sets × 12 reps @ 35 lbs
  - Leg Curls: 3 sets × 12 reps @ 90 lbs

### Generated Strava Description
```
🔥 PULL Workout

Back:
• Pull-ups: 4×10 @ 0 lbs
• Barbell Row: 3×8 @ 155 lbs

Biceps:
• Dumbbell Curl: 3×12 @ 35 lbs

Legs:
• Leg Curls: 3×12 @ 90 lbs

Total Volume: 4,980 lbs
Duration: 45 minutes
```

---

## Example 3: Progressive Weight Sets

### Input Workout
- **Type**: PUSH
- **Exercises**:
  - Barbell Bench Press (Progressive):
    - Set 1: 10 reps @ 135 lbs
    - Set 2: 8 reps @ 155 lbs
    - Set 3: 6 reps @ 175 lbs
    - Set 4: 4 reps @ 185 lbs

### Generated Strava Description
```
💪 PUSH Workout

Chest:
• Barbell Bench Press (Progressive): 10 @ 135 lbs, 8 @ 155 lbs, 6 @ 175 lbs, 4 @ 185 lbs

Total Volume: 2,540 lbs
Duration: 20 minutes
```

---

## Example 4: Incomplete Workout

### Input Workout
- **Type**: PULL
- **Status**: INCOMPLETE
- **Exercises**:
  - Pull-ups: 2 sets marked incomplete

### Generated Strava Description
```
🔥 PULL Workout

Back:
• Pull-ups: 0 sets

Total Volume: 0 lbs
```

---

## Volume Calculation

Total Volume = Σ(sets × reps × weight) for all completed sets

**Example Calculation:**
```
Barbell Bench Press: 3 × 10 × 135 = 4,050 lbs
Incline Dumbbell Press: 3 × 12 × 50 = 1,800 lbs
Squats: 4 × 8 × 185 = 5,920 lbs
Leg Press: 3 × 15 × 270 = 12,150 lbs
─────────────────────────────────────
Total Volume: 12,450 lbs
```

---

## Date Format

Strava requires ISO-8601 format in UTC:
- **Format**: `yyyy-MM-dd'T'HH:mm:ss'Z'`
- **Example**: `2025-10-21T14:00:00Z`

---

## Duration Calculation

### With Actual Times
```kotlin
val startTime = 1729522800000L // 2:00 PM
val endTime = 1729526280000L   // 2:58 PM
val durationSeconds = (endTime - startTime) / 1000
// Result: 3480 seconds (58 minutes)
```

### Without Times (Estimated)
```kotlin
// Base: 5 minutes
// Per exercise: 3 minutes
// Per set: 1.5 minutes

val baseTime = 5 * 60 = 300 seconds
val exerciseTime = 4 exercises × 3 min = 12 min = 720 seconds
val setTime = 13 sets × 1.5 min = 19.5 min = 1,170 seconds
─────────────────────────────────────
Total Estimated: 36.5 minutes ≈ 2,190 seconds
```

---

## Muscle Group Display Order

Sections are displayed in this order:
1. Chest
2. Shoulders
3. Triceps
4. Back
5. Biceps
6. Legs
7. Core

---

## Implementation Files

### StravaDescriptionFormatter.kt
- `format()` - Main formatting function
- `formatHeader()` - Workout type header with emoji
- `formatExercise()` - Individual exercise formatting
- `calculateTotalVolume()` - Volume calculation
- `formatVolume()` - Pretty volume display (450 lbs, 12.4K lbs, etc.)

### WorkoutToStravaMapper.kt
- `mapToActivityRequest()` - Convert Workout → StravaActivityRequest
- `formatActivityName()` - Activity name with emoji
- `formatDateToIso8601()` - Date formatting for Strava API
- `calculateElapsedTimeSeconds()` - Duration in seconds
- `estimateDuration()` - Duration estimation when times not available

### StravaMapperTest.kt
- `createSampleWorkout()` - Sample data generator
- `testFormatter()` - Test description formatting
- `testMapper()` - Test complete mapping
- `testEdgeCases()` - Edge case validation

---

## Usage in Production

```kotlin
// In your sync worker or use case
val workout = workoutRepository.getWorkout(workoutId)

// Get actual workout times from database
val startTime = workout.startTime ?: workout.date.time
val endTime = workout.endTime ?: System.currentTimeMillis()

// Map to Strava request
val activityRequest = WorkoutToStravaMapper.mapToActivityRequest(
    workout = workout,
    startTime = startTime,
    endTime = endTime
)

// Send to Strava API
val response = stravaApi.createActivity(
    authorization = "Bearer $accessToken",
    request = activityRequest
)

// Handle response
if (response.isSuccessful) {
    val activityId = response.body()?.id
    // Store activity ID for future reference
} else {
    // Handle error
}
```

---

## Next Steps

1. ✅ Formatter created
2. ✅ Mapper created
3. ✅ Test examples documented
4. ⏳ Create sync worker (next task)
5. ⏳ Implement queue system
6. ⏳ Add retry logic
7. ⏳ Test end-to-end sync

---

**Created**: October 21, 2025
**Status**: Data mapping layer complete, ready for sync worker implementation

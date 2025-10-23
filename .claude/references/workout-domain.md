# Workout Domain Logic & Business Rules

**When to load this reference:**
- Working on workout generation algorithm
- Implementing exercise selection or filtering
- Building workout tracking features
- Debugging 7-day cooldown system
- Working with Push/Pull alternation logic

**Load command:** Uncomment `@.claude/references/workout-domain.md` in `.claude/CLAUDE.md`

---

## Core Workout Concepts

### Push/Pull System
- **PUSH (Alpha Training)**: Chest, Shoulders, Triceps, Quads
- **PULL (Pack Strength)**: Back, Biceps, Hamstrings, Core

### Muscle Groups
```kotlin
enum class MuscleGroup {
    CHEST,      // Pectorals
    SHOULDER,   // Deltoids
    BACK,       // Lats, traps, rhomboids
    BICEP,      // Biceps
    TRICEP,     // Triceps
    LEGS,       // Quads, hamstrings, glutes, calves
    CORE        // Abs, obliques
}
```

### Workout Types
```kotlin
enum class WorkoutType {
    PUSH,       // Alpha Training
    PULL        // Pack Strength
}
```

### Workout Status
```kotlin
enum class WorkoutStatus {
    ACTIVE,      // Currently in progress
    COMPLETED,   // Finished and saved
    INCOMPLETE   // Saved with partial progress
}
```

## Business Rules

### Exercise Cooldown Logic

**Rule**: Exercises cannot be repeated within 7 days

**Implementation**:
```kotlin
fun getAvailableExercises(workoutType: WorkoutType): List<Exercise> {
    val sevenDaysAgo = LocalDate.now().minusDays(7)
    val recentExercises = workoutRepository.getExercisesSince(sevenDaysAgo)
    val recentExerciseIds = recentExercises.map { it.exerciseId }.toSet()

    return exerciseRepository.getExercisesByType(workoutType)
        .filter { it.id !in recentExerciseIds }
        .filter { it.isActive }
}
```

**Why 7 days?**
- Allows muscle recovery
- Ensures workout variety
- Prevents muscle adaptation/plateaus
- Keeps workouts fresh and engaging

### Workout Alternation Logic

**Rule**: Workouts alternate between PUSH and PULL

**Implementation**:
```kotlin
fun determineNextWorkoutType(): WorkoutType {
    val lastWorkout = workoutRepository.getLastCompletedWorkout()
    return when (lastWorkout?.type) {
        WorkoutType.PUSH -> WorkoutType.PULL
        WorkoutType.PULL -> WorkoutType.PUSH
        null -> WorkoutType.PUSH  // Default to PUSH for first workout
    }
}
```

**Alternation Benefits**:
- Balanced muscle development
- Adequate recovery time
- Prevents overtraining specific muscle groups
- Natural deload weeks when combined with cooldown

### Multi-Muscle Group Exercises

**Rule**: Exercises can target multiple muscle groups simultaneously

**Examples**:
- Bench Press → CHEST (primary), TRICEP, SHOULDER
- Deadlift → BACK (primary), LEGS, CORE
- Pull-ups → BACK (primary), BICEP

**Selection Priority**:
```kotlin
fun selectExerciseForMuscleGroup(
    targetGroup: MuscleGroup,
    availableExercises: List<Exercise>
): Exercise? {
    // Prefer exercises with target as primary muscle
    val primaryMatches = availableExercises.filter {
        it.muscleGroups.firstOrNull() == targetGroup
    }

    return primaryMatches.randomOrNull()
        ?: availableExercises.filter {
            targetGroup in it.muscleGroups
        }.randomOrNull()
}
```

### Gym Equipment Filtering

**Rule**: Equipment filter runs FIRST in priority chain

**Filter Priority Order**:
1. **Equipment** → Must match gym's available equipment
2. **Workout Type** → PUSH or PULL
3. **Cooldown** → Not done in last 7 days
4. **Muscle Groups** → Target 3 exercises per workout

**Smart Equipment Matching**:
```kotlin
// Barbell selection includes variants
BARBELL_VARIANTS = setOf(
    "Barbell",
    "Squat Bar",
    "Hex Bar",
    "Trap Bar",
    "EZ Bar"
)

// Bodyweight exercises always included
BODYWEIGHT_VARIANTS = setOf(
    "Bodyweight",
    "Body Only",
    "None"
)

fun canPerformExercise(
    exerciseEquipment: String,
    availableEquipment: List<String>
): Boolean {
    // Bodyweight exercises always available
    if (exerciseEquipment in BODYWEIGHT_VARIANTS) return true

    // Check if gym has matching equipment (including variants)
    return availableEquipment.any { gymEquipment ->
        matches(exerciseEquipment, gymEquipment)
    }
}
```

## Workout Generation Algorithm

### GenerateWorkoutUseCase

```kotlin
class GenerateWorkoutUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val gymRepository: GymRepository
) {
    suspend operator fun invoke(): List<Exercise> {
        // 1. Get default gym for equipment filtering
        val defaultGym = gymRepository.getDefaultGym()

        // 2. Determine workout type (alternates PUSH/PULL)
        val lastWorkout = workoutRepository.getLastWorkout()
        val workoutType = if (lastWorkout?.type == WorkoutType.PUSH) {
            WorkoutType.PULL
        } else {
            WorkoutType.PUSH
        }

        // 3. Get exercises done in last 7 days (cooldown)
        val recentExerciseIds = workoutRepository.getExerciseIdsFromLastWeek()

        // 4. Filter available exercises
        // PRIORITY: Equipment → Type → Cooldown → Muscle Groups
        val availableExercises = exerciseRepository
            .getUserActiveExercisesByType(workoutType)
            .filter { exercise ->
                // Equipment filter FIRST
                defaultGym?.let { gym ->
                    EquipmentType.canPerformExercise(
                        exercise.equipment,
                        gym.equipmentList
                    )
                } ?: true
            }
            .filterNot { it.id in recentExerciseIds }  // Cooldown filter

        // 5. Group by primary muscle group
        val exercisesByMuscle = availableExercises.groupBy {
            it.muscleGroups.firstOrNull() ?: MuscleGroup.CHEST
        }

        // 6. Select one exercise per target muscle group
        val targetMuscleGroups = if (workoutType == WorkoutType.PUSH) {
            listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDER, MuscleGroup.TRICEP)
        } else {
            listOf(MuscleGroup.LEGS, MuscleGroup.BACK, MuscleGroup.BICEP)
        }

        return targetMuscleGroups.mapNotNull { muscleGroup ->
            exercisesByMuscle[muscleGroup]?.randomOrNull()
        }.take(3)
    }
}
```

### Edge Cases

**Not Enough Available Exercises**:
```kotlin
// If fewer than 3 exercises available after filtering:
// 1. Return whatever is available (1-2 exercises)
// 2. User can manually add more exercises during workout
// 3. Consider relaxing cooldown or equipment filters
```

**No Exercises Match Criteria**:
```kotlin
// Fallback strategy:
// 1. First try: Ignore cooldown, keep equipment filter
// 2. Second try: Ignore equipment, keep cooldown
// 3. Last resort: Show all active exercises for workout type
```

## Workout Tracking

### Set/Rep/Weight Logging

**Data Model**:
```kotlin
data class Set(
    val reps: Int,
    val weight: Double,  // In lbs
    val completed: Boolean = false
)

data class WorkoutExercise(
    val exercise: Exercise,
    val sets: List<Set>,
    val orderIndex: Int,
    val totalVolume: Double  // Sum of (reps * weight) for all sets
)
```

**Volume Calculation**:
```kotlin
fun calculateTotalVolume(sets: List<Set>): Double {
    return sets
        .filter { it.completed }
        .sumOf { it.reps * it.weight }
}
```

### Workout Completion Rules

**Completed Workout**:
- User explicitly marks as "Complete"
- All exercises have at least one completed set
- Start and end times recorded
- Syncs to Strava (if connected)

**Incomplete Workout**:
- User saves progress without completing
- Preserves completed sets
- Can resume later or discard
- Does NOT sync to Strava

**Discarded Workout**:
- No data saved
- Doesn't count toward cooldown
- Doesn't affect alternation

## Exercise Shuffling

**Mid-Workout Exercise Swap**:
```kotlin
fun shuffleExercise(
    currentExercise: Exercise,
    workoutType: WorkoutType,
    availableExercises: List<Exercise>
): Exercise? {
    // Find replacement with same primary muscle group
    val targetMuscleGroup = currentExercise.muscleGroups.firstOrNull()

    return availableExercises
        .filter { it.id != currentExercise.id }  // Not same exercise
        .filter { it.muscleGroups.firstOrNull() == targetMuscleGroup }  // Same muscle
        .randomOrNull()
}
```

**Rules**:
- Must match primary muscle group
- Can violate 7-day cooldown (user override)
- Replacement options filtered by current gym equipment

## Total Volume Calculation

**Per Exercise**:
```kotlin
val exerciseVolume = sets
    .filter { it.completed }
    .sumOf { it.reps * it.weight }
```

**Per Workout**:
```kotlin
val workoutVolume = workout.exercises.sumOf { exercise ->
    exercise.sets
        .filter { it.completed }
        .sumOf { it.reps * it.weight }
}
```

**Display Format**:
```kotlin
fun formatVolume(volume: Double): String {
    return "${volume.toInt().toString().replace(
        Regex("(\\d)(?=(\\d{3})+$)"),
        "$1,"
    )} lbs"
}
// Example: 12450 → "12,450 lbs"
```

## Workout Duration

**Tracking**:
```kotlin
data class Workout(
    val startTime: Long?,  // Timestamp when workout started
    val endTime: Long?,    // Timestamp when completed
    // ...
) {
    val durationMinutes: Long?
        get() = if (startTime != null && endTime != null) {
            (endTime - startTime) / (60 * 1000)
        } else null
}
```

**Auto-Start**:
- Starts when user navigates to active workout
- Pauses if app backgrounded (optional)
- Completes when user marks workout as complete


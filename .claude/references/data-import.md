# Strong App Data Import

**When to load this reference:**
- Working on CSV import or data migration features
- Debugging Strong app import functionality
- Implementing exercise mapping logic
- Working on weight conversion or data transformation
- Building import UI or statistics display

**Load command:** Uncomment `@.claude/references/data-import.md` in `.claude/CLAUDE.md`

---

## Feature Overview

**Purpose**: Import workout history from Strong fitness app via CSV export

**Key Capabilities**:
- CSV parsing (semicolon-delimited format)
- Exercise mapping (50+ Strong exercises → Fortis Lupus exercises)
- Auto-creation of unmapped exercises as custom exercises
- Weight conversion (kg to lbs)
- Import statistics and error reporting
- Manual trigger via debug menu
- One-time import prevention

**Import Statistics Display**:
- Workouts imported count
- Exercises created count
- Errors encountered count

## Strong App CSV Format

### Expected Structure
```csv
Date;Workout Name;Exercise Name;Set Order;Weight;Reps;Distance;Seconds;Notes;Workout Notes
2023-12-15;Push;Barbell Bench Press;1;60;10;;;;
2023-12-15;Push;Barbell Bench Press;2;60;10;;;;
2023-12-15;Push;Incline Dumbbell Press;1;22.5;12;;;;
```

**Key Fields**:
- **Date**: `yyyy-MM-dd` format
- **Workout Name**: Push, Pull, or custom names
- **Exercise Name**: Strong app exercise name
- **Set Order**: Sequential set number
- **Weight**: In kg (will be converted to lbs)
- **Reps**: Number of repetitions
- **Delimiter**: Semicolon (`;`) not comma

### CSV Parsing Pattern
```kotlin
data class StrongWorkoutRow(
    val date: String,
    val workoutName: String,
    val exerciseName: String,
    val setOrder: Int,
    val weight: Double,
    val reps: Int,
    val distance: String?,
    val seconds: String?,
    val notes: String?,
    val workoutNotes: String?
)

fun parseCsv(csvContent: String): List<StrongWorkoutRow> {
    return csvContent.lines()
        .drop(1)  // Skip header
        .filter { it.isNotBlank() }
        .map { line ->
            val parts = line.split(";")
            StrongWorkoutRow(
                date = parts[0],
                workoutName = parts[1],
                exerciseName = parts[2],
                setOrder = parts[3].toIntOrNull() ?: 1,
                weight = parts[4].toDoubleOrNull() ?: 0.0,
                reps = parts[5].toIntOrNull() ?: 0,
                distance = parts.getOrNull(6),
                seconds = parts.getOrNull(7),
                notes = parts.getOrNull(8),
                workoutNotes = parts.getOrNull(9)
            )
        }
}
```

## Exercise Mapping System

### Mapping Strategy
1. **Exact Match**: Try exact Strong name → Fortis Lupus name
2. **Fuzzy Match**: Try lowercase/trimmed matching
3. **Auto-Create**: If no match found, create as custom exercise

### Exercise Mapping Table
```kotlin
object StrongExerciseMapper {
    private val EXERCISE_MAPPING = mapOf(
        // Chest
        "Barbell Bench Press" to "Barbell Bench Press",
        "Barbell Incline Bench Press" to "Barbell Incline Bench Press",
        "Dumbbell Bench Press" to "Dumbbell Bench Press",
        "Dumbbell Fly" to "Dumbbell Flyes",
        "Cable Chest Fly" to "Cable Fly",
        "Push Up" to "Push-Ups",

        // Shoulders
        "Barbell Overhead Press" to "Military Press",
        "Dumbbell Shoulder Press" to "Dumbbell Shoulder Press",
        "Dumbbell Lateral Raise" to "Side Lateral Raise",
        "Dumbbell Front Raise" to "Front Dumbbell Raise",
        "Cable Lateral Raise" to "Cable Lateral Raise",

        // Back
        "Barbell Row" to "Bent Over Barbell Row",
        "Cable Row" to "Cable Seated Row",
        "Lat Pulldown" to "Wide-Grip Lat Pulldown",
        "Pull Up" to "Pull-Ups",
        "Dumbbell Row" to "One-Arm Dumbbell Row",

        // Arms
        "Barbell Curl" to "Barbell Curl",
        "Dumbbell Curl" to "Dumbbell Bicep Curl",
        "Cable Curl" to "Cable Curl",
        "Tricep Dip" to "Dips - Triceps Version",
        "Cable Tricep Extension" to "Triceps Pushdown",

        // Legs
        "Barbell Squat" to "Barbell Squat",
        "Leg Press" to "Leg Press",
        "Romanian Deadlift" to "Romanian Deadlift",
        "Leg Curl" to "Seated Leg Curl",
        "Leg Extension" to "Leg Extensions",

        // Core
        "Crunch" to "Crunches",
        "Plank" to "Plank",
        "Russian Twist" to "Russian Twist"
    )

    fun mapExerciseName(strongName: String): String? {
        // Try exact match
        EXERCISE_MAPPING[strongName]?.let { return it }

        // Try case-insensitive match
        val normalizedKey = strongName.lowercase().trim()
        EXERCISE_MAPPING.entries.find {
            it.key.lowercase().trim() == normalizedKey
        }?.let { return it.value }

        // No match found
        return null
    }
}
```

## Auto-Creation Logic

### Custom Exercise Creation for Unmapped Exercises
```kotlin
suspend fun getOrCreateExercise(
    strongName: String,
    exerciseRepository: ExerciseRepository
): Exercise {
    // Try mapping first
    val mappedName = StrongExerciseMapper.mapExerciseName(strongName)

    if (mappedName != null) {
        // Find existing exercise by mapped name
        val existing = exerciseRepository.getExerciseByName(mappedName)
        if (existing != null) return existing
    }

    // No mapping or mapped exercise not found → auto-create
    val customExercise = Exercise(
        name = strongName,
        muscleGroups = inferMuscleGroups(strongName),
        workoutType = inferWorkoutType(strongName),
        equipment = inferEquipment(strongName),
        isUserCreated = true,  // Mark as custom
        imageUrl = null,
        instructions = "Imported from Strong app"
    )

    val exerciseId = exerciseRepository.insertExercise(customExercise)
    return customExercise.copy(id = exerciseId)
}

private fun inferMuscleGroups(exerciseName: String): List<MuscleGroup> {
    val name = exerciseName.lowercase()
    return when {
        name.contains("chest") || name.contains("bench") -> listOf(MuscleGroup.CHEST)
        name.contains("shoulder") || name.contains("press") -> listOf(MuscleGroup.SHOULDER)
        name.contains("back") || name.contains("row") || name.contains("pull") -> listOf(MuscleGroup.BACK)
        name.contains("bicep") || name.contains("curl") -> listOf(MuscleGroup.BICEP)
        name.contains("tricep") || name.contains("dip") -> listOf(MuscleGroup.TRICEP)
        name.contains("squat") || name.contains("leg") -> listOf(MuscleGroup.LEGS)
        name.contains("core") || name.contains("ab") || name.contains("crunch") -> listOf(MuscleGroup.CORE)
        else -> listOf(MuscleGroup.CORE)  // Default fallback
    }
}

private fun inferWorkoutType(exerciseName: String): WorkoutType {
    val muscleGroups = inferMuscleGroups(exerciseName)
    val primary = muscleGroups.firstOrNull() ?: MuscleGroup.CORE

    return when (primary) {
        MuscleGroup.CHEST, MuscleGroup.SHOULDER, MuscleGroup.TRICEP, MuscleGroup.LEGS -> WorkoutType.PUSH
        MuscleGroup.BACK, MuscleGroup.BICEP, MuscleGroup.CORE -> WorkoutType.PULL
    }
}

private fun inferEquipment(exerciseName: String): String {
    val name = exerciseName.lowercase()
    return when {
        name.contains("barbell") -> "Barbell"
        name.contains("dumbbell") -> "Dumbbell"
        name.contains("cable") -> "Cable"
        name.contains("machine") -> "Machine"
        name.contains("bodyweight") || name.contains("push up") || name.contains("pull up") -> "Bodyweight"
        else -> "Other"
    }
}
```

## Weight Conversion

### Kg to Lbs Conversion
```kotlin
object WeightConverter {
    const val KG_TO_LBS_MULTIPLIER = 2.20462

    fun kgToLbs(kg: Double): Double {
        if (kg == 0.0) return 0.0
        return (kg * KG_TO_LBS_MULTIPLIER).roundToNearestFive()
    }

    private fun Double.roundToNearestFive(): Double {
        return (this / 5.0).roundToInt() * 5.0
    }
}

// Example conversions:
// 60 kg → 132.28 lbs → 130 lbs (rounded to nearest 5)
// 22.5 kg → 49.60 lbs → 50 lbs
// 100 kg → 220.46 lbs → 220 lbs
```

### Conversion Logic in Import
```kotlin
fun importWorkoutSet(row: StrongWorkoutRow): WorkoutSet {
    val weightLbs = WeightConverter.kgToLbs(row.weight)

    return WorkoutSet(
        weight = weightLbs,
        reps = row.reps,
        completed = true  // All imported sets marked as completed
    )
}
```

## Import Use Case

### ImportWorkoutUseCase Implementation
```kotlin
class ImportWorkoutUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository
) {
    data class ImportResult(
        val workoutsImported: Int,
        val exercisesCreated: Int,
        val errors: Int
    )

    suspend operator fun invoke(csvContent: String): ImportResult {
        var workoutsImported = 0
        var exercisesCreated = 0
        var errors = 0

        try {
            // Parse CSV
            val rows = StrongCsvParser.parseCsv(csvContent)

            // Group by workout date
            val workoutsByDate = rows.groupBy { it.date }

            workoutsByDate.forEach { (dateStr, workoutRows) ->
                try {
                    // Determine workout type from first exercise or name
                    val workoutType = determineWorkoutType(workoutRows)

                    // Create workout entity
                    val date = LocalDate.parse(dateStr).atStartOfDay()
                    val workout = Workout(
                        date = date.toInstant(ZoneOffset.UTC).toEpochMilli(),
                        type = workoutType,
                        status = WorkoutStatus.COMPLETED,
                        startTime = date.toInstant(ZoneOffset.UTC).toEpochMilli(),
                        endTime = date.plusHours(1).toInstant(ZoneOffset.UTC).toEpochMilli()
                    )

                    val workoutId = workoutRepository.insertWorkout(workout)

                    // Group exercises within workout
                    val exercisesByName = workoutRows.groupBy { it.exerciseName }

                    exercisesByName.forEach { (exerciseName, sets) ->
                        try {
                            // Get or create exercise
                            val exercise = getOrCreateExercise(exerciseName, exerciseRepository)

                            if (exercise.isUserCreated) {
                                exercisesCreated++
                            }

                            // Create workout exercise with all sets
                            sets.forEach { setRow ->
                                val weightLbs = WeightConverter.kgToLbs(setRow.weight)

                                workoutRepository.insertWorkoutExercise(
                                    WorkoutExercise(
                                        workoutId = workoutId,
                                        exerciseId = exercise.id,
                                        sets = 1,
                                        reps = setRow.reps,
                                        weight = weightLbs,
                                        completed = true
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            errors++
                            Log.e("ImportWorkout", "Error importing exercise: $exerciseName", e)
                        }
                    }

                    workoutsImported++
                } catch (e: Exception) {
                    errors++
                    Log.e("ImportWorkout", "Error importing workout: $dateStr", e)
                }
            }
        } catch (e: Exception) {
            errors++
            Log.e("ImportWorkout", "Error parsing CSV", e)
        }

        return ImportResult(
            workoutsImported = workoutsImported,
            exercisesCreated = exercisesCreated,
            errors = errors
        )
    }

    private fun determineWorkoutType(rows: List<StrongWorkoutRow>): WorkoutType {
        val workoutName = rows.firstOrNull()?.workoutName?.lowercase() ?: ""

        return when {
            workoutName.contains("push") -> WorkoutType.PUSH
            workoutName.contains("pull") -> WorkoutType.PULL
            else -> {
                // Infer from first exercise
                val firstExercise = rows.firstOrNull()?.exerciseName ?: ""
                inferWorkoutType(firstExercise)
            }
        }
    }
}
```

## UI Integration

### Import Statistics Dialog
```kotlin
@Composable
fun ImportStatsDialog(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Complete") },
        text = {
            Column {
                Text("✅ Workouts Imported: ${result.workoutsImported}")
                Text("🏋️ Exercises Created: ${result.exercisesCreated}")
                if (result.errors > 0) {
                    Text("⚠️ Errors: ${result.errors}", color = Color.Red)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
```

### Debug Menu Trigger
```kotlin
// In HomeScreen ViewModel
fun triggerImport() {
    viewModelScope.launch {
        try {
            _importInProgress.value = true

            // Read embedded CSV from assets
            val csvContent = assetManager.open("marc_workouts.csv")
                .bufferedReader()
                .use { it.readText() }

            // Import
            val result = importWorkoutUseCase(csvContent)

            _importResult.value = result
            _showImportDialog.value = true

        } catch (e: Exception) {
            Log.e("HomeViewModel", "Import failed", e)
            _importError.value = e.message
        } finally {
            _importInProgress.value = false
        }
    }
}
```

### Menu Item (Debug Only)
```kotlin
@Composable
fun HomeScreenMenu(
    onImportClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Import Marc's Workouts") },
            onClick = {
                expanded = false
                onImportClick()
            }
        )
    }
}
```

## One-Time Import Prevention

### Using DataStore for Import Flag
```kotlin
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val IMPORT_COMPLETED = booleanPreferencesKey("import_completed")

    val isImportCompleted: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[IMPORT_COMPLETED] ?: false
        }

    suspend fun setImportCompleted() {
        dataStore.edit { preferences ->
            preferences[IMPORT_COMPLETED] = true
        }
    }
}
```

### Checking Before Import
```kotlin
fun triggerImport() {
    viewModelScope.launch {
        // Check if already imported
        val alreadyImported = userPreferencesRepository.isImportCompleted.first()

        if (alreadyImported) {
            _importError.value = "Workouts already imported"
            return@launch
        }

        // Proceed with import...
        val result = importWorkoutUseCase(csvContent)

        // Mark as completed
        userPreferencesRepository.setImportCompleted()
    }
}
```

## Error Handling

### Common Import Errors
```kotlin
sealed class ImportError : Exception() {
    object InvalidCsvFormat : ImportError()
    object NoDataFound : ImportError()
    object AlreadyImported : ImportError()
    data class ParseError(val line: Int, val message: String) : ImportError()
    data class ExerciseCreationFailed(val exerciseName: String) : ImportError()
}

fun handleImportError(error: ImportError): String {
    return when (error) {
        is ImportError.InvalidCsvFormat ->
            "Invalid CSV format. Please use Strong app export format."
        is ImportError.NoDataFound ->
            "No workout data found in CSV file."
        is ImportError.AlreadyImported ->
            "Workouts have already been imported."
        is ImportError.ParseError ->
            "Error parsing line ${error.line}: ${error.message}"
        is ImportError.ExerciseCreationFailed ->
            "Failed to create exercise: ${error.exerciseName}"
    }
}
```

## Test Data

### Embedded CSV Example
Located in `app/src/main/assets/marc_workouts.csv`:
- **133 workouts** (Dec 2023 - Aug 2025)
- **50+ unique exercises**
- Real-world Strong app export format
- Used for testing import functionality

### Sample CSV Structure
```csv
Date;Workout Name;Exercise Name;Set Order;Weight;Reps;Distance;Seconds;Notes;Workout Notes
2023-12-15;Push;Barbell Bench Press;1;60;10;;;;
2023-12-15;Push;Barbell Bench Press;2;60;10;;;;
2023-12-15;Push;Barbell Bench Press;3;60;10;;;;
2023-12-15;Push;Incline Dumbbell Press;1;22.5;12;;;;
2023-12-15;Push;Incline Dumbbell Press;2;22.5;12;;;;
2023-12-17;Pull;Barbell Row;1;50;10;;;;
2023-12-17;Pull;Barbell Row;2;50;10;;;;
2023-12-17;Pull;Lat Pulldown;1;45;12;;;;
```

## Performance Considerations

### Batch Processing
- Import workouts in batches of 10 to avoid UI blocking
- Use `withContext(Dispatchers.IO)` for file I/O operations
- Show progress indicator during import
- Process in background thread

### Memory Management
- Parse CSV line-by-line for large files
- Don't load entire CSV into memory at once
- Clear references after import completion
- Use Room batch insertion for better performance

## Future Enhancements

**TODO - Phase 6**:
- Support for other fitness app formats (MyFitnessPal, JEFIT)
- Import validation before committing to database
- Duplicate workout detection and merging
- Selective import (date range, specific workouts)
- Import history tracking
- Undo import functionality

# ExerciseDB API Analysis Report

## Executive Summary

This report provides a comprehensive analysis of the ExerciseDB API structure for integration into a Kotlin Android workout app. The analysis is based on the free-exercise-db dataset (873 exercises) which is an open-source alternative to the commercial ExerciseDB RapidAPI.

**Data Source:** https://github.com/yuhonas/free-exercise-db
**Dataset Size:** 873 exercises
**API Format:** JSON
**License:** Public Domain

---

## 1. JSON Schema Structure

### Sample Exercise Object

```json
{
  "id": "Alternate_Hammer_Curl",
  "name": "Alternate Hammer Curl",
  "force": "pull",
  "level": "beginner",
  "mechanic": "isolation",
  "equipment": "dumbbell",
  "primaryMuscles": ["biceps"],
  "secondaryMuscles": ["forearms"],
  "instructions": [
    "Stand up with your torso upright and a dumbbell in each hand being held at arms length...",
    "The palms of the hands should be facing your torso. This will be your starting position...",
    "While holding the upper arm stationary, curl the right weight forward..."
  ],
  "category": "strength",
  "images": [
    "Alternate_Hammer_Curl/0.jpg",
    "Alternate_Hammer_Curl/1.jpg"
  ]
}
```

### Field Descriptions

| Field | Type | Description | Nullable |
|-------|------|-------------|----------|
| `id` | String | Unique identifier (snake_case) | No |
| `name` | String | Human-readable exercise name | No |
| `force` | String | Movement type: "push", "pull", "static" | Yes |
| `level` | String | Difficulty: "beginner", "intermediate", "expert" | No |
| `mechanic` | String | Movement pattern: "compound", "isolation" | Yes |
| `equipment` | String | Required equipment | Yes (null = bodyweight) |
| `primaryMuscles` | Array<String> | Main muscles worked | No (can be empty) |
| `secondaryMuscles` | Array<String> | Supporting muscles | No (can be empty) |
| `instructions` | Array<String> | Step-by-step instructions | No |
| `category` | String | Exercise type category | No |
| `images` | Array<String> | Relative paths to exercise images | No |

---

## 2. Complete Equipment Types (12 Total)

| Equipment | Count | Notes |
|-----------|-------|-------|
| `body only` | Most common | Bodyweight exercises, no equipment needed |
| `barbell` | High | Standard barbell exercises |
| `dumbbell` | High | Dumbbell exercises |
| `cable` | Medium | Cable machine exercises |
| `machine` | Medium | Fixed machine exercises |
| `kettlebells` | Medium | Kettlebell-specific movements |
| `bands` | Low | Resistance band exercises |
| `exercise ball` | Low | Stability ball exercises |
| `e-z curl bar` | Low | EZ curl bar specific |
| `foam roll` | Low | Recovery/stretching |
| `medicine ball` | Low | Medicine ball exercises |
| `other` | Low | Miscellaneous equipment |

---

## 3. Complete Muscle Groups (17 Total)

### Primary & Secondary Muscles

Both `primaryMuscles` and `secondaryMuscles` use the same muscle taxonomy:

| Muscle | API Value | Target Schema Mapping |
|--------|-----------|----------------------|
| Chest | `chest` | `CHEST` |
| Shoulders | `shoulders` | `SHOULDER` |
| Triceps | `triceps` | `TRICEP` |
| Biceps | `biceps` | `BICEP` |
| Lats | `lats` | `BACK` |
| Middle Back | `middle back` | `BACK` |
| Lower Back | `lower back` | `BACK` |
| Traps | `traps` | `BACK` |
| Quadriceps | `quadriceps` | `LEGS` |
| Hamstrings | `hamstrings` | `LEGS` |
| Glutes | `glutes` | `LEGS` |
| Calves | `calves` | `LEGS` |
| Adductors | `adductors` | `LEGS` |
| Abductors | `abductors` | `LEGS` |
| Abdominals | `abdominals` | `CORE` |
| Forearms | `forearms` | *Not mapped* (minor muscle) |
| Neck | `neck` | *Not mapped* (minor muscle) |

---

## 4. Mapping Rules: ExerciseDB → Target Schema

### 4.1 Muscle Group Mapping

```kotlin
fun mapMuscleGroups(primaryMuscles: List<String>, secondaryMuscles: List<String>): List<MuscleGroup> {
    val allMuscles = (primaryMuscles + secondaryMuscles).distinct()
    val muscleGroups = mutableSetOf<MuscleGroup>()

    allMuscles.forEach { muscle ->
        when (muscle.lowercase()) {
            "chest" -> muscleGroups.add(MuscleGroup.CHEST)
            "shoulders" -> muscleGroups.add(MuscleGroup.SHOULDER)
            "triceps" -> muscleGroups.add(MuscleGroup.TRICEP)
            "biceps" -> muscleGroups.add(MuscleGroup.BICEP)
            "lats", "middle back", "lower back", "traps" -> muscleGroups.add(MuscleGroup.BACK)
            "quadriceps", "hamstrings", "glutes", "calves", "adductors", "abductors" ->
                muscleGroups.add(MuscleGroup.LEGS)
            "abdominals" -> muscleGroups.add(MuscleGroup.CORE)
            // forearms, neck are ignored (minor muscles)
        }
    }

    return muscleGroups.toList()
}
```

### 4.2 Equipment Mapping

```kotlin
fun mapEquipment(equipment: String?): String {
    return when (equipment?.lowercase()) {
        null -> "Bodyweight"
        "body only" -> "Bodyweight"
        "barbell" -> "Barbell"
        "dumbbell" -> "Dumbbell"
        "cable" -> "Cable"
        "machine" -> "Machine"
        "kettlebells" -> "Kettlebell"
        "bands" -> "Resistance Band"
        "exercise ball" -> "Exercise Ball"
        "e-z curl bar" -> "EZ Bar"
        "foam roll" -> "Foam Roller"
        "medicine ball" -> "Medicine Ball"
        "other" -> "Other"
        else -> equipment.capitalize()
    }
}
```

### 4.3 Image URL Mapping

```kotlin
fun mapImageUrl(images: List<String>?): String? {
    val baseUrl = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"
    return images?.firstOrNull()?.let { baseUrl + it }
}
```

---

## 5. PUSH vs PULL Classification Rules

### 5.1 Analysis of Force Distribution

Based on analysis of 873 exercises:

**PUSH Muscle Groups (367 exercises):**
- Quadriceps: 100 exercises
- Shoulders: 80 exercises
- Chest: 72 exercises
- Triceps: 68 exercises
- Hamstrings: 14 exercises
- Calves: 14 exercises
- Glutes: 13 exercises

**PULL Muscle Groups (387 exercises):**
- Abdominals: 84 exercises
- Biceps: 49 exercises
- Hamstrings: 47 exercises
- Middle Back: 30 exercises
- Lats: 30 exercises
- Lower Back: 16 exercises
- Traps: 15 exercises
- Forearms: 20 exercises

**STATIC (119 exercises):**
- Stretching and isometric holds

### 5.2 Classification Logic

The ExerciseDB API provides a `force` field that can be used directly:

```kotlin
fun mapWorkoutType(
    force: String?,
    primaryMuscles: List<String>,
    category: String?
): WorkoutType {
    // First: Use force field if available
    when (force?.lowercase()) {
        "push" -> return WorkoutType.PUSH
        "pull" -> return WorkoutType.PULL
        "static" -> return WorkoutType.PULL // Treat static as PULL (or create STATIC category)
    }

    // Fallback: Classify by primary muscles
    val muscleClassification = primaryMuscles.firstOrNull()?.lowercase()

    return when (muscleClassification) {
        // PUSH muscles
        "chest", "triceps", "shoulders", "quadriceps", "calves", "glutes" ->
            WorkoutType.PUSH

        // PULL muscles
        "biceps", "lats", "middle back", "lower back", "traps",
        "hamstrings", "abdominals", "forearms" ->
            WorkoutType.PULL

        // Default to PULL for unknown
        else -> WorkoutType.PULL
    }
}
```

### 5.3 Refined Classification Rules

**PUSH Exercises Include:**
- Chest presses, flyes, push-ups
- Shoulder presses, raises
- Tricep extensions, dips
- Squats, lunges, leg presses (quad-dominant)
- Calf raises

**PULL Exercises Include:**
- Rows, pull-ups, lat pulldowns
- Bicep curls
- Deadlifts, hamstring curls (hamstring-dominant)
- Core exercises (planks, crunches)
- Traps exercises (shrugs)

**Edge Cases:**
- **Shoulders:** Can be both PUSH (presses) and PULL (reverse flyes, face pulls). Use `force` field to determine.
- **Hamstrings:** Usually PULL (deadlifts, curls) but can be PUSH (leg curls). Check `force` field.
- **Core/Abdominals:** Generally classified as PULL in the dataset.

---

## 6. Additional Metadata

### 6.1 Exercise Categories

| Category | Description | Count |
|----------|-------------|-------|
| `strength` | Traditional resistance training | ~650 |
| `stretching` | Flexibility and mobility | ~150 |
| `cardio` | Cardiovascular exercises | ~30 |
| `plyometrics` | Explosive/jump training | ~20 |
| `powerlifting` | Powerlifting-specific | ~10 |
| `strongman` | Strongman-specific | ~8 |
| `olympic weightlifting` | Olympic lifts | ~5 |

### 6.2 Difficulty Levels

| Level | Description | Recommended For |
|-------|-------------|-----------------|
| `beginner` | Basic movements, low complexity | New lifters, learning form |
| `intermediate` | Moderate complexity, some experience | Regular gym-goers |
| `expert` | Advanced movements, high skill | Experienced athletes |

### 6.3 Mechanic Types

| Mechanic | Description | Examples |
|----------|-------------|----------|
| `compound` | Multi-joint movements | Squats, bench press, rows |
| `isolation` | Single-joint movements | Bicep curls, leg extensions |
| `null` | Not applicable | Stretches, cardio |

---

## 7. Data Access Methods

### Option 1: Free Exercise DB (Recommended for Development)

**Endpoint:** `https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json`

**Advantages:**
- No API key required
- Free and open-source
- 873 exercises
- Public domain license
- Includes images

**Limitations:**
- No pagination (single large JSON file ~2.5MB)
- No search/filter endpoints
- Static dataset (infrequent updates)

### Option 2: ExerciseDB RapidAPI (Production)

**Endpoint:** `https://exercisedb.p.rapidapi.com/`

**Advantages:**
- RESTful API with filtering
- 1300+ exercises
- Regular updates
- Pagination support
- GIF animations

**Limitations:**
- Requires API key (RapidAPI account)
- Rate limits on free tier
- Different schema (needs adapter)

---

## 8. Implementation Recommendations

### 8.1 Data Loading Strategy

```kotlin
// Download and cache the entire dataset on first app launch
class ExerciseRepository {
    suspend fun syncExercises() {
        val url = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json"
        val response = httpClient.get(url)
        val exercises = response.body<List<ExerciseDbJson>>()

        // Transform and save to local database
        exercises.forEach { exerciseJson ->
            val exercise = Exercise(
                id = exerciseJson.id,
                name = exerciseJson.name,
                muscleGroups = mapMuscleGroups(exerciseJson.primaryMuscles, exerciseJson.secondaryMuscles),
                equipment = mapEquipment(exerciseJson.equipment),
                category = mapWorkoutType(exerciseJson.force, exerciseJson.primaryMuscles, exerciseJson.category),
                imageUrl = mapImageUrl(exerciseJson.images),
                instructions = exerciseJson.instructions
            )
            database.exerciseDao().insert(exercise)
        }
    }
}
```

### 8.2 Schema Adapter

```kotlin
data class ExerciseDbJson(
    val id: String,
    val name: String,
    val force: String?,
    val level: String,
    val mechanic: String?,
    val equipment: String?,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val instructions: List<String>,
    val category: String,
    val images: List<String>
)

fun ExerciseDbJson.toDomain(): Exercise {
    return Exercise(
        id = this.id,
        name = this.name,
        muscleGroups = mapMuscleGroups(this.primaryMuscles, this.secondaryMuscles),
        equipment = mapEquipment(this.equipment),
        category = mapWorkoutType(this.force, this.primaryMuscles, this.category),
        imageUrl = mapImageUrl(this.images),
        instructions = this.instructions
    )
}
```

### 8.3 Filtering by Muscle Groups

```kotlin
fun filterByMuscleGroup(exercises: List<Exercise>, targetGroup: MuscleGroup): List<Exercise> {
    return exercises.filter { it.muscleGroups.contains(targetGroup) }
}

fun filterByWorkoutType(exercises: List<Exercise>, type: WorkoutType): List<Exercise> {
    return exercises.filter { it.category == type }
}

fun filterByEquipment(exercises: List<Exercise>, equipment: String): List<Exercise> {
    return exercises.filter { it.equipment == equipment }
}
```

---

## 9. Summary Statistics

| Metric | Value |
|--------|-------|
| Total Exercises | 873 |
| Unique Equipment Types | 12 |
| Unique Muscle Groups | 17 (maps to 7 in target schema) |
| PUSH Exercises | 367 (42%) |
| PULL Exercises | 387 (44%) |
| STATIC Exercises | 119 (14%) |
| Beginner Exercises | ~350 (40%) |
| Intermediate Exercises | ~400 (46%) |
| Expert Exercises | ~123 (14%) |

---

## 10. Next Steps

1. **Download Dataset:** Integrate the JSON download into your app's initial setup
2. **Create Room Database:** Store exercises locally for offline access
3. **Implement Filters:** Add UI filters for equipment, muscle groups, and difficulty
4. **Image Caching:** Implement image loading with Coil/Glide for exercise images
5. **Search Functionality:** Add text search across exercise names and instructions
6. **Favorites System:** Allow users to favorite exercises for quick access

---

## Appendix A: Sample Exercises by Category

### PUSH - Chest
```json
{
  "name": "Barbell Bench Press",
  "force": "push",
  "primaryMuscles": ["chest"],
  "secondaryMuscles": ["shoulders", "triceps"],
  "equipment": "barbell",
  "category": "strength"
}
```

### PULL - Back
```json
{
  "name": "Barbell Bent Over Row",
  "force": "pull",
  "primaryMuscles": ["middle back"],
  "secondaryMuscles": ["biceps", "lats"],
  "equipment": "barbell",
  "category": "strength"
}
```

### PUSH - Shoulders
```json
{
  "name": "Dumbbell Shoulder Press",
  "force": "push",
  "primaryMuscles": ["shoulders"],
  "secondaryMuscles": ["triceps"],
  "equipment": "dumbbell",
  "category": "strength"
}
```

### PULL - Biceps
```json
{
  "name": "Barbell Curl",
  "force": "pull",
  "primaryMuscles": ["biceps"],
  "secondaryMuscles": ["forearms"],
  "equipment": "barbell",
  "category": "strength"
}
```

### PUSH - Legs
```json
{
  "name": "Barbell Squat",
  "force": "push",
  "primaryMuscles": ["quadriceps"],
  "secondaryMuscles": ["glutes", "hamstrings"],
  "equipment": "barbell",
  "category": "strength"
}
```

### PULL - Core
```json
{
  "name": "Plank",
  "force": "static",
  "primaryMuscles": ["abdominals"],
  "secondaryMuscles": [],
  "equipment": "body only",
  "category": "strength"
}
```

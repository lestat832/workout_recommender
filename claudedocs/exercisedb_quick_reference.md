# ExerciseDB API - Quick Reference Guide

## 📊 Dataset Overview

- **Source:** https://github.com/yuhonas/free-exercise-db
- **Total Exercises:** 873
- **API Endpoint:** `https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json`
- **License:** Public Domain (Free to use)

---

## 🔗 JSON Schema

```json
{
  "id": "Barbell_Bench_Press",
  "name": "Barbell Bench Press",
  "force": "push",                    // "push" | "pull" | "static"
  "level": "beginner",                // "beginner" | "intermediate" | "expert"
  "mechanic": "compound",             // "compound" | "isolation" | null
  "equipment": "barbell",             // See equipment list below
  "primaryMuscles": ["chest"],        // Array of muscle names
  "secondaryMuscles": ["triceps", "shoulders"],
  "instructions": ["Step 1...", "Step 2..."],
  "category": "strength",             // See category list below
  "images": ["Barbell_Bench_Press/0.jpg", "Barbell_Bench_Press/1.jpg"]
}
```

---

## 💪 Muscle Group Mapping

### Target Schema (7 Groups)
```
CHEST | SHOULDER | TRICEP | BICEP | BACK | LEGS | CORE
```

### ExerciseDB → Target Mapping

| ExerciseDB Muscle | Target Group |
|------------------|--------------|
| `chest` | `CHEST` |
| `shoulders` | `SHOULDER` |
| `triceps` | `TRICEP` |
| `biceps` | `BICEP` |
| `lats`, `middle back`, `lower back`, `traps` | `BACK` |
| `quadriceps`, `hamstrings`, `glutes`, `calves`, `adductors`, `abductors` | `LEGS` |
| `abdominals` | `CORE` |
| `forearms`, `neck` | *Ignore (minor muscles)* |

---

## 🏋️ Equipment Types (12 Total)

| ExerciseDB Value | Display Name |
|-----------------|--------------|
| `null` or `"body only"` | Bodyweight |
| `"barbell"` | Barbell |
| `"dumbbell"` | Dumbbell |
| `"cable"` | Cable |
| `"machine"` | Machine |
| `"kettlebells"` | Kettlebell |
| `"bands"` | Resistance Band |
| `"exercise ball"` | Exercise Ball |
| `"e-z curl bar"` | EZ Bar |
| `"foam roll"` | Foam Roller |
| `"medicine ball"` | Medicine Ball |
| `"other"` | Other |

---

## 🔄 PUSH vs PULL Classification

### Method 1: Use `force` Field (Recommended)

```kotlin
when (exercise.force?.lowercase()) {
    "push" -> WorkoutType.PUSH
    "pull" -> WorkoutType.PULL
    "static" -> WorkoutType.PULL  // or create STATIC category
}
```

### Method 2: Fallback by Primary Muscle

**PUSH Muscles** (42% of exercises)
```
chest, triceps, shoulders, quadriceps, calves, glutes
```

**PULL Muscles** (44% of exercises)
```
biceps, lats, middle back, lower back, traps, hamstrings, abdominals, forearms
```

**STATIC** (14% of exercises)
```
Stretches, isometric holds
```

---

## 📋 Exercise Categories

| Category | Description | Count |
|----------|-------------|-------|
| `strength` | Resistance training | ~650 |
| `stretching` | Flexibility/mobility | ~150 |
| `cardio` | Cardiovascular | ~30 |
| `plyometrics` | Explosive/jump | ~20 |
| `powerlifting` | Powerlifting-specific | ~10 |
| `strongman` | Strongman-specific | ~8 |
| `olympic weightlifting` | Olympic lifts | ~5 |

---

## 🖼️ Image URLs

**Base URL:**
```
https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/
```

**Example:**
```kotlin
val imageUrl = baseUrl + exercise.images[0]
// Result: https://raw.githubusercontent.com/.../Barbell_Bench_Press/0.jpg
```

---

## 🎯 Common Filter Patterns

### Get CHEST PUSH exercises with BARBELL
```kotlin
exercises
    .filter { it.muscleGroups.contains(MuscleGroup.CHEST) }
    .filter { it.category == WorkoutType.PUSH }
    .filter { it.equipment == "Barbell" }
```

### Get beginner BODYWEIGHT exercises
```kotlin
exercises
    .filter { it.difficulty == DifficultyLevel.BEGINNER }
    .filter { it.equipment == "Bodyweight" }
```

### Get COMPOUND LEG exercises
```kotlin
exercises
    .filter { it.muscleGroups.contains(MuscleGroup.LEGS) }
    .filter { it.mechanic == MechanicType.COMPOUND }
```

---

## 🚀 Quick Implementation Checklist

- [ ] Add Ktor HTTP client dependency
- [ ] Add Kotlinx Serialization dependency
- [ ] Create `ExerciseDbJson` data class with `@Serializable`
- [ ] Create mapping functions (`mapMuscleGroups`, `mapEquipment`, etc.)
- [ ] Create `ExerciseDbService` to fetch JSON
- [ ] Create Room database entities and DAO
- [ ] Create `ExerciseRepository` for data access
- [ ] Implement sync function to download and cache exercises
- [ ] Create ViewModel for UI integration
- [ ] Add Coil/Glide for image loading
- [ ] Implement filtering UI

---

## 📦 Required Dependencies

```kotlin
// build.gradle.kts (app)
dependencies {
    // Ktor for HTTP
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // Room for local database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
}
```

---

## 🔍 Sample Data Examples

### PUSH Exercise
```json
{
  "name": "Barbell Bench Press",
  "force": "push",
  "primaryMuscles": ["chest"],
  "secondaryMuscles": ["shoulders", "triceps"],
  "equipment": "barbell"
}
```
**Maps to:** `WorkoutType.PUSH`, `MuscleGroup.CHEST`

### PULL Exercise
```json
{
  "name": "Barbell Bent Over Row",
  "force": "pull",
  "primaryMuscles": ["middle back"],
  "secondaryMuscles": ["biceps", "lats"],
  "equipment": "barbell"
}
```
**Maps to:** `WorkoutType.PULL`, `MuscleGroup.BACK`

### LEGS Exercise
```json
{
  "name": "Barbell Squat",
  "force": "push",
  "primaryMuscles": ["quadriceps"],
  "secondaryMuscles": ["glutes", "hamstrings"],
  "equipment": "barbell"
}
```
**Maps to:** `WorkoutType.PUSH`, `MuscleGroup.LEGS`

---

## 🎨 Muscle Distribution Insights

### Most Common PUSH Muscles
1. Quadriceps (100 exercises)
2. Shoulders (80 exercises)
3. Chest (72 exercises)
4. Triceps (68 exercises)

### Most Common PULL Muscles
1. Abdominals (84 exercises)
2. Biceps (49 exercises)
3. Hamstrings (47 exercises)
4. Lats (30 exercises)
5. Middle Back (30 exercises)

---

## ⚠️ Edge Cases to Handle

1. **Null `force` field:** Some exercises have `force: null` → Use muscle-based classification
2. **Null `equipment` field:** Treat as bodyweight exercise
3. **Empty muscle arrays:** Handle gracefully, log warning
4. **Multiple muscle groups:** Exercise can target multiple groups (e.g., compound movements)
5. **Shoulders in both PUSH and PULL:** Trust `force` field for classification

---

## 💡 Pro Tips

1. **Cache everything:** Download all 873 exercises once, store locally
2. **Offline-first:** Use Room database as single source of truth
3. **Image optimization:** Use Coil with disk caching for images
4. **Search optimization:** Create FTS table in Room for fast text search
5. **Background sync:** Use WorkManager to refresh data periodically
6. **Multi-muscle filtering:** Show exercises that work ANY of selected muscle groups
7. **Compound preference:** Prioritize compound exercises for efficiency
8. **Equipment availability:** Let users mark available equipment for filtering

---

## 📞 Support Resources

- **GitHub:** https://github.com/yuhonas/free-exercise-db
- **Issues:** https://github.com/yuhonas/free-exercise-db/issues
- **Alternative API:** https://exercisedb.p.rapidapi.com (requires API key)
- **Documentation:** https://www.exercisedb.dev/docs

---

## 🆚 ExerciseDB Comparison

| Feature | free-exercise-db | RapidAPI ExerciseDB |
|---------|-----------------|---------------------|
| Cost | Free | Free tier + paid |
| Exercises | 873 | 1300+ |
| API Key | Not required | Required |
| Rate Limits | None | Yes (free tier) |
| Updates | Infrequent | Regular |
| Images | JPG (2 per exercise) | GIF animations |
| Schema | Simple | More detailed |
| Best For | Development, MVPs | Production apps |

**Recommendation:** Start with free-exercise-db for development, consider RapidAPI for production if you need more exercises.

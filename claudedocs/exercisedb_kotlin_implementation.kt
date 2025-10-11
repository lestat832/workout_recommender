/**
 * ExerciseDB API Integration - Kotlin Implementation Reference
 *
 * This file contains ready-to-use Kotlin code for integrating the ExerciseDB API
 * into your Android workout app.
 */

package com.example.workout_app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================================================
// 1. DATA MODELS
// ============================================================================

/**
 * Target domain model for your app
 */
data class Exercise(
    val id: String,
    val name: String,
    val muscleGroups: List<MuscleGroup>,
    val equipment: String,
    val category: WorkoutType,
    val imageUrl: String?,
    val instructions: List<String>,
    val difficulty: DifficultyLevel? = null,
    val mechanic: MechanicType? = null
)

/**
 * ExerciseDB API response model
 */
@Serializable
data class ExerciseDbJson(
    @SerialName("id")
    val id: String,

    @SerialName("name")
    val name: String,

    @SerialName("force")
    val force: String? = null,

    @SerialName("level")
    val level: String,

    @SerialName("mechanic")
    val mechanic: String? = null,

    @SerialName("equipment")
    val equipment: String? = null,

    @SerialName("primaryMuscles")
    val primaryMuscles: List<String>,

    @SerialName("secondaryMuscles")
    val secondaryMuscles: List<String>,

    @SerialName("instructions")
    val instructions: List<String>,

    @SerialName("category")
    val category: String,

    @SerialName("images")
    val images: List<String>
)

/**
 * Enums for your target schema
 */
enum class MuscleGroup {
    CHEST,
    SHOULDER,
    TRICEP,
    LEGS,
    BACK,
    BICEP,
    CORE
}

enum class WorkoutType {
    PUSH,
    PULL
}

enum class DifficultyLevel {
    BEGINNER,
    INTERMEDIATE,
    EXPERT
}

enum class MechanicType {
    COMPOUND,
    ISOLATION
}

// ============================================================================
// 2. MAPPING FUNCTIONS
// ============================================================================

/**
 * Extension function to convert ExerciseDB JSON to domain Exercise model
 */
fun ExerciseDbJson.toDomain(): Exercise {
    return Exercise(
        id = this.id,
        name = this.name,
        muscleGroups = mapMuscleGroups(this.primaryMuscles, this.secondaryMuscles),
        equipment = mapEquipment(this.equipment),
        category = mapWorkoutType(this.force, this.primaryMuscles),
        imageUrl = mapImageUrl(this.images),
        instructions = this.instructions,
        difficulty = mapDifficulty(this.level),
        mechanic = mapMechanic(this.mechanic)
    )
}

/**
 * Maps ExerciseDB muscle names to your app's MuscleGroup enum
 * Combines both primary and secondary muscles for comprehensive targeting
 */
fun mapMuscleGroups(
    primaryMuscles: List<String>,
    secondaryMuscles: List<String>
): List<MuscleGroup> {
    val allMuscles = (primaryMuscles + secondaryMuscles).distinct()
    val muscleGroups = mutableSetOf<MuscleGroup>()

    allMuscles.forEach { muscle ->
        when (muscle.lowercase()) {
            "chest" -> muscleGroups.add(MuscleGroup.CHEST)

            "shoulders" -> muscleGroups.add(MuscleGroup.SHOULDER)

            "triceps" -> muscleGroups.add(MuscleGroup.TRICEP)

            "biceps" -> muscleGroups.add(MuscleGroup.BICEP)

            "lats", "middle back", "lower back", "traps" ->
                muscleGroups.add(MuscleGroup.BACK)

            "quadriceps", "hamstrings", "glutes", "calves", "adductors", "abductors" ->
                muscleGroups.add(MuscleGroup.LEGS)

            "abdominals" -> muscleGroups.add(MuscleGroup.CORE)

            // forearms, neck are ignored (minor muscles)
        }
    }

    return muscleGroups.toList()
}

/**
 * Maps ExerciseDB equipment names to user-friendly strings
 */
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
        else -> equipment.replaceFirstChar { it.uppercase() }
    }
}

/**
 * Determines if exercise is PUSH or PULL
 * Uses force field first, falls back to muscle-based classification
 */
fun mapWorkoutType(
    force: String?,
    primaryMuscles: List<String>
): WorkoutType {
    // First: Use force field if available
    when (force?.lowercase()) {
        "push" -> return WorkoutType.PUSH
        "pull" -> return WorkoutType.PULL
        "static" -> return WorkoutType.PULL // Treat static as PULL (mostly core/stretching)
    }

    // Fallback: Classify by primary muscle
    val primaryMuscle = primaryMuscles.firstOrNull()?.lowercase()

    return when (primaryMuscle) {
        // PUSH muscles: pressing/extending movements
        "chest", "triceps", "shoulders", "quadriceps", "calves", "glutes" ->
            WorkoutType.PUSH

        // PULL muscles: pulling/flexing movements
        "biceps", "lats", "middle back", "lower back", "traps",
        "hamstrings", "abdominals", "forearms" ->
            WorkoutType.PULL

        // Default to PULL for unknown
        else -> WorkoutType.PULL
    }
}

/**
 * Maps difficulty level
 */
fun mapDifficulty(level: String): DifficultyLevel {
    return when (level.lowercase()) {
        "beginner" -> DifficultyLevel.BEGINNER
        "intermediate" -> DifficultyLevel.INTERMEDIATE
        "expert" -> DifficultyLevel.EXPERT
        else -> DifficultyLevel.BEGINNER
    }
}

/**
 * Maps mechanic type
 */
fun mapMechanic(mechanic: String?): MechanicType? {
    return when (mechanic?.lowercase()) {
        "compound" -> MechanicType.COMPOUND
        "isolation" -> MechanicType.ISOLATION
        else -> null
    }
}

/**
 * Constructs full image URL from relative path
 */
fun mapImageUrl(images: List<String>?): String? {
    val baseUrl = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"
    return images?.firstOrNull()?.let { baseUrl + it }
}

// ============================================================================
// 3. API SERVICE
// ============================================================================

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Service for fetching exercises from ExerciseDB API
 */
class ExerciseDbService(
    private val httpClient: HttpClient = createHttpClient()
) {
    companion object {
        private const val BASE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist"

        fun createHttpClient(): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
            }
        }
    }

    /**
     * Fetches all exercises from the API
     */
    suspend fun fetchAllExercises(): Result<List<ExerciseDbJson>> {
        return try {
            val exercises = httpClient.get("$BASE_URL/exercises.json")
                .body<List<ExerciseDbJson>>()
            Result.success(exercises)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ============================================================================
// 4. REPOSITORY
// ============================================================================

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing exercise data
 */
class ExerciseRepository(
    private val apiService: ExerciseDbService,
    private val exerciseDao: ExerciseDao
) {
    /**
     * Syncs exercises from API to local database
     * Call this on first app launch or to refresh data
     */
    suspend fun syncExercises(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Fetch from API
            val result = apiService.fetchAllExercises()

            if (result.isFailure) {
                return@withContext Result.failure(result.exceptionOrNull()!!)
            }

            val exercisesJson = result.getOrNull() ?: emptyList()

            // Transform to domain models
            val exercises = exercisesJson.map { it.toDomain() }

            // Save to database
            exerciseDao.insertAll(exercises)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets all exercises from local database
     */
    suspend fun getAllExercises(): List<Exercise> = withContext(Dispatchers.IO) {
        exerciseDao.getAllExercises()
    }

    /**
     * Filters exercises by muscle group
     */
    suspend fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.getByMuscleGroup(muscleGroup)
        }

    /**
     * Filters exercises by workout type (PUSH/PULL)
     */
    suspend fun getExercisesByWorkoutType(type: WorkoutType): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.getByWorkoutType(type)
        }

    /**
     * Filters exercises by equipment
     */
    suspend fun getExercisesByEquipment(equipment: String): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.getByEquipment(equipment)
        }

    /**
     * Search exercises by name
     */
    suspend fun searchExercises(query: String): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.searchByName(query)
        }
}

// ============================================================================
// 5. ROOM DATABASE (DAO)
// ============================================================================

import androidx.room.*
import androidx.room.Dao

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<Exercise>

    @Query("SELECT * FROM exercises WHERE muscleGroups LIKE '%' || :muscleGroup || '%'")
    suspend fun getByMuscleGroup(muscleGroup: MuscleGroup): List<Exercise>

    @Query("SELECT * FROM exercises WHERE category = :workoutType")
    suspend fun getByWorkoutType(workoutType: WorkoutType): List<Exercise>

    @Query("SELECT * FROM exercises WHERE equipment = :equipment")
    suspend fun getByEquipment(equipment: String): List<Exercise>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%'")
    suspend fun searchByName(query: String): List<Exercise>

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()
}

// ============================================================================
// 6. FILTERING EXTENSIONS
// ============================================================================

/**
 * Extension functions for filtering exercise lists
 */
fun List<Exercise>.filterByMuscleGroup(targetGroup: MuscleGroup): List<Exercise> {
    return filter { it.muscleGroups.contains(targetGroup) }
}

fun List<Exercise>.filterByWorkoutType(type: WorkoutType): List<Exercise> {
    return filter { it.category == type }
}

fun List<Exercise>.filterByEquipment(equipment: String): List<Exercise> {
    return filter { it.equipment.equals(equipment, ignoreCase = true) }
}

fun List<Exercise>.filterByDifficulty(difficulty: DifficultyLevel): List<Exercise> {
    return filter { it.difficulty == difficulty }
}

fun List<Exercise>.filterByMechanic(mechanic: MechanicType): List<Exercise> {
    return filter { it.mechanic == mechanic }
}

/**
 * Multi-criteria filtering
 */
fun List<Exercise>.filterBy(
    muscleGroups: List<MuscleGroup>? = null,
    workoutType: WorkoutType? = null,
    equipment: List<String>? = null,
    difficulty: DifficultyLevel? = null,
    mechanic: MechanicType? = null
): List<Exercise> {
    var filtered = this

    muscleGroups?.let { groups ->
        filtered = filtered.filter { exercise ->
            exercise.muscleGroups.any { it in groups }
        }
    }

    workoutType?.let {
        filtered = filtered.filter { it.category == workoutType }
    }

    equipment?.let { equipmentList ->
        filtered = filtered.filter { it.equipment in equipmentList }
    }

    difficulty?.let {
        filtered = filtered.filter { it.difficulty == difficulty }
    }

    mechanic?.let {
        filtered = filtered.filter { it.mechanic == mechanic }
    }

    return filtered
}

// ============================================================================
// 7. USAGE EXAMPLES
// ============================================================================

/**
 * Example: Sync exercises on app startup
 */
suspend fun exampleSyncExercises(repository: ExerciseRepository) {
    val result = repository.syncExercises()

    if (result.isSuccess) {
        println("Successfully synced exercises")
    } else {
        println("Failed to sync: ${result.exceptionOrNull()?.message}")
    }
}

/**
 * Example: Get all PUSH exercises for CHEST
 */
suspend fun exampleGetChestPushExercises(repository: ExerciseRepository) {
    val allExercises = repository.getAllExercises()

    val chestPushExercises = allExercises
        .filterByWorkoutType(WorkoutType.PUSH)
        .filterByMuscleGroup(MuscleGroup.CHEST)

    println("Found ${chestPushExercises.size} chest push exercises")
}

/**
 * Example: Get beginner bodyweight exercises
 */
suspend fun exampleGetBeginnerBodyweightExercises(repository: ExerciseRepository) {
    val allExercises = repository.getAllExercises()

    val beginnerBodyweight = allExercises
        .filterByDifficulty(DifficultyLevel.BEGINNER)
        .filterByEquipment("Bodyweight")

    println("Found ${beginnerBodyweight.size} beginner bodyweight exercises")
}

/**
 * Example: Multi-criteria filtering
 */
suspend fun exampleAdvancedFiltering(repository: ExerciseRepository) {
    val allExercises = repository.getAllExercises()

    val filtered = allExercises.filterBy(
        muscleGroups = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEP),
        workoutType = WorkoutType.PUSH,
        equipment = listOf("Barbell", "Dumbbell"),
        difficulty = DifficultyLevel.INTERMEDIATE,
        mechanic = MechanicType.COMPOUND
    )

    println("Found ${filtered.size} exercises matching all criteria")
}

// ============================================================================
// 8. VIEW MODEL EXAMPLE
// ============================================================================

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ExerciseViewModel(
    private val repository: ExerciseRepository
) : ViewModel() {

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadExercises()
    }

    fun loadExercises() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Try to load from local database first
                var localExercises = repository.getAllExercises()

                // If empty, sync from API
                if (localExercises.isEmpty()) {
                    val syncResult = repository.syncExercises()
                    if (syncResult.isFailure) {
                        _error.value = "Failed to load exercises: ${syncResult.exceptionOrNull()?.message}"
                        return@launch
                    }
                    localExercises = repository.getAllExercises()
                }

                _exercises.value = localExercises
            } catch (e: Exception) {
                _error.value = "Error loading exercises: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterByMuscleGroup(muscleGroup: MuscleGroup) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val filtered = repository.getExercisesByMuscleGroup(muscleGroup)
                _exercises.value = filtered
            } catch (e: Exception) {
                _error.value = "Error filtering exercises: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterByWorkoutType(type: WorkoutType) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val filtered = repository.getExercisesByWorkoutType(type)
                _exercises.value = filtered
            } catch (e: Exception) {
                _error.value = "Error filtering exercises: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchExercises(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = repository.searchExercises(query)
                _exercises.value = results
            } catch (e: Exception) {
                _error.value = "Error searching exercises: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

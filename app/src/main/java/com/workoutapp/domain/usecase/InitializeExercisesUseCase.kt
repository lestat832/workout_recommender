package com.workoutapp.domain.usecase

import android.content.Context
import android.content.SharedPreferences
import com.workoutapp.data.database.CustomExerciseSeeder
import com.workoutapp.data.database.HomeGymCatalogSeeder
import com.workoutapp.data.database.HomeGymMovementCatalog
import com.workoutapp.data.repository.ExerciseRepositoryImpl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to initialize exercises on first app launch
 * Handles loading from ExerciseDB and merging with curated exercises
 */
class InitializeExercisesUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepositoryImpl,
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("workout_app_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_EXERCISES_INITIALIZED = "exercises_initialized"
        private const val KEY_EXERCISE_COUNT = "exercise_count"
        private const val KEY_CATEGORY_BACKFILLED = "exercise_category_backfilled"
        private const val KEY_CONDITIONING_EXERCISES_SEEDED = "conditioning_exercises_seeded"
        private const val KEY_CARDIO_NAMES_RECLASSIFIED = "cardio_names_reclassified"
        private const val KEY_HOME_GYM_CATALOG_SEEDED = "home_gym_catalog_seeded"
    }

    /**
     * Initializes exercises if not already done
     * This will be called on app startup
     *
     * @return Result with number of exercises loaded, or null if already initialized
     */
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<Int?> = withContext(Dispatchers.IO) {
        try {
            val isInitialized = prefs.getBoolean(KEY_EXERCISES_INITIALIZED, false)

            val count: Int? = if (isInitialized && !forceRefresh) {
                null
            } else {
                val result = exerciseRepository.syncExercisesFromApi()
                if (result.isFailure) {
                    return@withContext Result.failure(
                        result.exceptionOrNull() ?: Exception("Failed to sync exercises")
                    )
                }
                val c = result.getOrNull() ?: 0
                prefs.edit()
                    .putBoolean(KEY_EXERCISES_INITIALIZED, true)
                    .putInt(KEY_EXERCISE_COUNT, c)
                    .apply()
                c
            }

            // One-time backfill of exerciseCategory for all existing exercises.
            // Runs once per install, independent of the CDN sync flag, so existing
            // users who already initialized before this code shipped still get it.
            if (!prefs.getBoolean(KEY_CATEGORY_BACKFILLED, false)) {
                exerciseRepository.backfillExerciseCategories()
                prefs.edit().putBoolean(KEY_CATEGORY_BACKFILLED, true).apply()
            }

            // Phase 3: one-time seed of the original custom conditioning exercises
            // (TRX, medicine ball, ab wheel, cardio stations). Most of these are
            // superseded by the Phase 3.1 catalog below, but we still insert them
            // so IDs stay stable for any historical workouts that reference them.
            if (!prefs.getBoolean(KEY_CONDITIONING_EXERCISES_SEEDED, false)) {
                exerciseRepository.insertExercises(CustomExerciseSeeder.exercises)
                prefs.edit().putBoolean(KEY_CONDITIONING_EXERCISES_SEEDED, true).apply()
            }

            // Phase 3.1 fix 1: reclassify cardio/plyometric exercises that were
            // previously miscategorized as STRENGTH_LEGS or similar due to their
            // muscle group tags (e.g., "Fast Skipping" showing up on pull day).
            // Runs once per install regardless of the backfill flag.
            if (!prefs.getBoolean(KEY_CARDIO_NAMES_RECLASSIFIED, false)) {
                exerciseRepository.reclassifyCardioByName()
                prefs.edit().putBoolean(KEY_CARDIO_NAMES_RECLASSIFIED, true).apply()
            }

            // Phase 3.1 fix 2: seed the Home Gym movement catalog and activate
            // everything (catalog + Phase 3 orphans) in user_exercises. The
            // original Phase 3 seed inserted rows into `exercises` but never
            // marked them active in `user_exercises`, so the generator's join
            // filtered them out — that's the "No exercises available" error.
            // This gate fixes both: the new catalog AND the old orphans.
            if (!prefs.getBoolean(KEY_HOME_GYM_CATALOG_SEEDED, false)) {
                val catalogExercises = HomeGymCatalogSeeder.buildExercises()
                exerciseRepository.insertExercises(catalogExercises)

                val catalogIds = HomeGymMovementCatalog.movements.map { it.id }
                val phase3OrphanIds = CustomExerciseSeeder.exercises.map { it.id }
                val allIds = (catalogIds + phase3OrphanIds).distinct()
                exerciseRepository.setUserExercises(allIds)

                prefs.edit().putBoolean(KEY_HOME_GYM_CATALOG_SEEDED, true).apply()
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Forces a refresh of exercises from API
     */
    suspend fun forceRefresh(): Result<Int> {
        return invoke(forceRefresh = true).map { it ?: 0 }
    }

    /**
     * Checks if exercises have been initialized
     */
    fun isInitialized(): Boolean {
        return prefs.getBoolean(KEY_EXERCISES_INITIALIZED, false)
    }

    /**
     * Gets the cached exercise count
     */
    fun getCachedExerciseCount(): Int {
        return prefs.getInt(KEY_EXERCISE_COUNT, 0)
    }

    /**
     * Resets initialization state (for testing/debugging)
     */
    fun resetInitialization() {
        prefs.edit()
            .putBoolean(KEY_EXERCISES_INITIALIZED, false)
            .putInt(KEY_EXERCISE_COUNT, 0)
            .apply()
    }
}

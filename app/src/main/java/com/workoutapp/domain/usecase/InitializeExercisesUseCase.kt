package com.workoutapp.domain.usecase

import android.content.Context
import android.content.SharedPreferences
import com.workoutapp.data.database.CustomExerciseSeeder
import com.workoutapp.data.database.HomeGymCatalogSeeder
import com.workoutapp.data.database.HomeGymMovementCatalog
import com.workoutapp.data.database.LmuLegCatalog
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
        private const val KEY_LMU_LEG_CATALOG_SEEDED = "lmu_leg_catalog_seeded"
        private const val KEY_HOME_GYM_UPPER_EXPANSION_SEEDED = "home_gym_upper_expansion_seeded"
        private const val KEY_HOME_GYM_POOL_EXPANSION_2_SEEDED = "home_gym_pool_expansion_2_seeded"
        private const val KEY_HOME_GYM_POOL_EXPANSION_3_SEEDED = "home_gym_pool_expansion_3_seeded"

        // Ids added after the initial Home Gym catalog seed. Existing installs
        // need a one-shot insert + activation for these; fresh installs pick
        // them up via the full catalog seed above.
        private val UPPER_EXPANSION_IDS = setOf(
            "custom_pull_up",
            "custom_atomic_pushup_trx",
            "custom_atomic_pushup_sliders"
        )

        // Second pool expansion — 16 movements sourced from user's written
        // list and the Stack 52 TRX poster. Spans all 5 non-cardio buckets.
        // Each migration gets its own gate for auditability; don't merge with
        // UPPER_EXPANSION_IDS. (custom_trx_lunge_twist was part of this batch
        // but was later removed from the catalog.)
        private val POOL_EXPANSION_2_IDS = setOf(
            "custom_heavy_db_deadlift",
            "custom_pair_db_rdl",
            "custom_trx_side_lunge",
            "custom_trx_hamstring_curl",
            "custom_trx_hip_press",
            "custom_trx_single_arm_row",
            "custom_trx_y_fly",
            "custom_trx_power_pull",
            "custom_trx_squat_row",
            "custom_trx_t_pushup",
            "custom_trx_spiderman_pushup",
            "custom_trx_clap_pushup",
            "custom_side_plank",
            "custom_trx_oblique_crunch",
            "custom_squat_thrusts",
            "custom_trx_mountain_climber"
        )

        // Third pool expansion — 12 movements added after the first feedback
        // loop: forward lunge variants (Heavy DB + Pair DB + bodyweight reverse),
        // kettlebell additions (swing, single-leg deadlift, windmill, clean,
        // snatch), medicine ball core work (russian twist, woodchopper, sit-up),
        // and wall ball squats as a self-toss full-body compound. Does NOT
        // include medicine ball chest pass (no wall space) or ball slam
        // (explicitly removed).
        private val POOL_EXPANSION_3_IDS = setOf(
            "custom_heavy_db_forward_lunge",
            "custom_pair_db_forward_lunge",
            "custom_kb_swing",
            "custom_kb_single_leg_deadlift",
            "custom_kb_windmill",
            "custom_kb_clean",
            "custom_kb_snatch",
            "custom_med_ball_russian_twist",
            "custom_med_ball_woodchopper",
            "custom_med_ball_sit_up",
            "custom_reverse_lunges",
            "custom_wall_ball_squats"
        )
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

            // Upper-body expansion: Pull-Up and Atomic Push-Up were added to
            // HomeGymMovementCatalog after the initial seed shipped. Existing
            // installs already flipped KEY_HOME_GYM_CATALOG_SEEDED so they
            // need an additive seed for just these two ids. Fresh installs
            // already inserted them above and this block is a no-op update
            // (insertExercises uses REPLACE conflict strategy).
            if (!prefs.getBoolean(KEY_HOME_GYM_UPPER_EXPANSION_SEEDED, false)) {
                val newExercises = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in UPPER_EXPANSION_IDS }
                exerciseRepository.insertExercises(newExercises)
                exerciseRepository.setUserExercises(UPPER_EXPANSION_IDS.toList())
                prefs.edit().putBoolean(KEY_HOME_GYM_UPPER_EXPANSION_SEEDED, true).apply()
            }

            // Pool expansion #2: 16 movements across lower/pull/push/core/
            // conditioning buckets, sourced from Marc's written list plus
            // curated TRX picks from the Stack 52 poster. Same additive-seed
            // pattern as the upper-body expansion above.
            if (!prefs.getBoolean(KEY_HOME_GYM_POOL_EXPANSION_2_SEEDED, false)) {
                val newExercises = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in POOL_EXPANSION_2_IDS }
                exerciseRepository.insertExercises(newExercises)
                exerciseRepository.setUserExercises(POOL_EXPANSION_2_IDS.toList())
                prefs.edit().putBoolean(KEY_HOME_GYM_POOL_EXPANSION_2_SEEDED, true).apply()
            }

            // Pool expansion #3: 12 movements added after the first feedback
            // loop — forward lunges, kettlebell work, medicine ball core, and
            // self-toss wall ball squats. Follows the same additive-seed
            // pattern; each migration flag is independent for auditability.
            if (!prefs.getBoolean(KEY_HOME_GYM_POOL_EXPANSION_3_SEEDED, false)) {
                val newExercises = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in POOL_EXPANSION_3_IDS }
                exerciseRepository.insertExercises(newExercises)
                exerciseRepository.setUserExercises(POOL_EXPANSION_3_IDS.toList())
                prefs.edit().putBoolean(KEY_HOME_GYM_POOL_EXPANSION_3_SEEDED, true).apply()
            }

            // LMU leg catalog: hand-curated list of leg movements that may
            // surface on LMU pull day. Seeds the 9 new movements (the other 2
            // are reused from the Home Gym catalog) and activates all 11 ids.
            if (!prefs.getBoolean(KEY_LMU_LEG_CATALOG_SEEDED, false)) {
                val newLmuLegs = LmuLegCatalog.buildNewExercises()
                exerciseRepository.insertExercises(newLmuLegs)
                exerciseRepository.setUserExercises(LmuLegCatalog.allowedIds.toList())
                prefs.edit().putBoolean(KEY_LMU_LEG_CATALOG_SEEDED, true).apply()
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

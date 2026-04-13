package com.workoutapp.domain.usecase

import android.content.Context
import android.content.SharedPreferences
import com.workoutapp.data.database.CustomExerciseSeeder
import com.workoutapp.data.database.ExerciseDataV2
import com.workoutapp.data.database.HomeGymCatalogSeeder
import com.workoutapp.data.database.HomeGymMovementCatalog
import com.workoutapp.data.database.LmuLegCatalog
import com.workoutapp.data.repository.ExerciseRepositoryImpl
import com.workoutapp.domain.model.Set
import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutExercise
import com.workoutapp.domain.model.WorkoutFormat
import com.workoutapp.domain.model.WorkoutStatus
import com.workoutapp.domain.model.WorkoutType
import com.workoutapp.domain.repository.GymRepository
import com.workoutapp.domain.repository.UserPreferencesRepository
import com.workoutapp.domain.repository.WorkoutRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

/**
 * UseCase to initialize exercises on first app launch
 * Handles loading from ExerciseDB and merging with curated exercises
 */
class InitializeExercisesUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepositoryImpl,
    @ApplicationContext private val context: Context,
    private val hevyHistorySeeder: HevyHistorySeeder,
    private val gymRepository: GymRepository,
    private val workoutRepository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val profileComputerUseCase: ProfileComputerUseCase
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
        private const val KEY_HEVY_HISTORY_SEEDED = "hevy_history_seeded"
        private const val KEY_GYM_DEDUP_APPLIED = "gym_dedup_applied"
        private const val KEY_HOME_GYM_HISTORY_SEEDED = "home_gym_history_seeded"
        private const val KEY_LMU_BODYWEIGHT_REMOVED = "lmu_bodyweight_removed"
        private const val KEY_EXERCISEDB_ACTIVATED = "exercisedb_activated"
        private const val KEY_LMU_BENCH_20260412_SEEDED = "lmu_bench_20260412_seeded"

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

            // Hevy history seed: one-time import of 201 historical workout
            // sessions from bundled CSV (res/raw/hevy_export.csv). Creates
            // exercises + completed workouts + recomputes training profile.
            // Runs after all exercise catalogs are seeded so mappings resolve.
            if (!prefs.getBoolean(KEY_HEVY_HISTORY_SEEDED, false)) {
                // All Hevy history is from the LMU gym — look up its ID
                // so workouts show under the correct gym in Pack History.
                val lmuGym = gymRepository.getAllGyms().firstOrNull { it.name == "LMU Gym" }
                hevyHistorySeeder.seedFromBundledCsv(gymId = lmuGym?.id)
                prefs.edit().putBoolean(KEY_HEVY_HISTORY_SEEDED, true).apply()
            }

            // Auto-complete onboarding since the home screen gym selector
            // replaces the old FTUE. This ensures isOnboardingComplete() is
            // true for any code that still checks it.
            userPreferencesRepository.markOnboardingComplete()

            // Gym dedup: the original FTUE created a gym via CreateGymUseCase
            // on top of the two DB-seeded gyms, producing duplicates (e.g. two
            // "Home Gym" entries). This one-shot cleanup merges duplicates by
            // name, reassigning workouts and updating selectedGymId before
            // deleting the extras.
            if (!prefs.getBoolean(KEY_GYM_DEDUP_APPLIED, false)) {
                deduplicateGyms()
                prefs.edit().putBoolean(KEY_GYM_DEDUP_APPLIED, true).apply()
            }

            // Remove "Bodyweight" and "None" from LMU's equipment list so
            // bodyweight exercises don't appear in LMU strength workouts.
            // Equipment.kt no longer unconditionally includes bodyweight —
            // gyms must explicitly list "Bodyweight" to get those exercises.
            if (!prefs.getBoolean(KEY_LMU_BODYWEIGHT_REMOVED, false)) {
                val lmuGymForEquip = gymRepository.getAllGyms().firstOrNull { it.name == "LMU Gym" }
                if (lmuGymForEquip != null) {
                    val filtered = lmuGymForEquip.equipmentList.filterNot { it in listOf("Bodyweight", "None") }
                    gymRepository.updateGym(lmuGymForEquip.copy(equipmentList = filtered))
                }
                prefs.edit().putBoolean(KEY_LMU_BODYWEIGHT_REMOVED, true).apply()
            }

            // Activate ExerciseDataV2 exercises in user_exercises so the
            // workout generator can find them. Previously only HomeGymCatalog
            // and LmuLegCatalog exercises were activated, leaving LMU with
            // almost no BACK/BICEP/CHEST/SHOULDER/TRICEP candidates.
            if (!prefs.getBoolean(KEY_EXERCISEDB_ACTIVATED, false)) {
                val exerciseDbIds = ExerciseDataV2.exercises.map { it.id }
                exerciseRepository.setUserExercises(exerciseDbIds)
                prefs.edit().putBoolean(KEY_EXERCISEDB_ACTIVATED, true).apply()
            }

            // Seed 2 real Home Gym conditioning workouts so Pack History
            // isn't empty on first launch. Uses deterministic IDs for
            // idempotency and rollback identification.
            if (!prefs.getBoolean(KEY_HOME_GYM_HISTORY_SEEDED, false)) {
                seedHomeGymHistory()
                prefs.edit().putBoolean(KEY_HOME_GYM_HISTORY_SEEDED, true).apply()
            }

            // Seed today's LMU bench press workout from Hevy (2026-04-12).
            // Single exercise, 11 sets — pyramid up to 225 and back down.
            if (!prefs.getBoolean(KEY_LMU_BENCH_20260412_SEEDED, false)) {
                seedLmuBench20260412()
                prefs.edit().putBoolean(KEY_LMU_BENCH_20260412_SEEDED, true).apply()
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deduplicateGyms() {
        val allGyms = gymRepository.getAllGyms()
        val grouped = allGyms.groupBy { it.name }
        val storedGymId = userPreferencesRepository.selectedGymId().firstOrNull()

        for ((_, gymsWithSameName) in grouped) {
            if (gymsWithSameName.size <= 1) continue

            // Keep the lowest-id entry (the DB-seeded original)
            val sorted = gymsWithSameName.sortedBy { it.id }
            val keeper = sorted.first()
            val duplicates = sorted.drop(1)

            for (dup in duplicates) {
                // Re-point any workouts referencing the duplicate
                workoutRepository.reassignWorkouts(dup.id, keeper.id)

                // Update stored preference if it pointed to the duplicate
                if (storedGymId == dup.id) {
                    userPreferencesRepository.setSelectedGymId(keeper.id)
                }

                gymRepository.deleteGym(dup)
            }

            // Ensure the keeper is default if none of the remaining gyms are
            if (!keeper.isDefault && duplicates.any { it.isDefault }) {
                gymRepository.setDefaultGym(keeper.id)
            }
        }
    }

    private suspend fun seedHomeGymHistory() {
        val homeGym = gymRepository.getAllGyms().firstOrNull { it.name == "Home Gym" } ?: return
        val gymId = homeGym.id

        fun makeDate(year: Int, month: Int, day: Int) = Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        data class ExerciseSpec(val id: String, val reps: Int, val weight: Float)

        suspend fun seedWorkout(
            workoutId: String,
            date: java.util.Date,
            format: WorkoutFormat,
            durationMinutes: Int,
            completedRounds: Int?,
            exercises: List<ExerciseSpec>
        ) {
            // All-or-nothing: look up all exercises first
            val resolved = exercises.mapNotNull { spec ->
                exerciseRepository.getExerciseById(spec.id)?.let { exercise -> spec to exercise }
            }
            if (resolved.size != exercises.size) return // skip if any lookup failed

            val workout = Workout(
                id = workoutId,
                date = date,
                type = WorkoutType.PULL,
                status = WorkoutStatus.COMPLETED,
                gymId = gymId,
                format = format,
                durationMinutes = durationMinutes,
                completedRounds = completedRounds
            )
            workoutRepository.createWorkout(workout)

            for ((spec, exercise) in resolved) {
                val we = WorkoutExercise(
                    id = UUID.randomUUID().toString(),
                    workoutId = workoutId,
                    exercise = exercise,
                    sets = listOf(Set(reps = spec.reps, weight = spec.weight, completed = true))
                )
                workoutRepository.addExerciseToWorkout(workoutId, we)
            }
        }

        // Workout 1: 04/12/2026 AMRAP 15 min, 5 rounds
        seedWorkout(
            workoutId = "home_gym_seed_amrap_20260412",
            date = makeDate(2026, 4, 12),
            format = WorkoutFormat.AMRAP,
            durationMinutes = 15,
            completedRounds = 5,
            exercises = listOf(
                ExerciseSpec("custom_row_200_400m", reps = 1, weight = 0f),
                ExerciseSpec("custom_pair_db_squat", reps = 10, weight = 25f),
                ExerciseSpec("custom_trx_row", reps = 12, weight = 0f),
                ExerciseSpec("custom_pushup", reps = 12, weight = 0f)
            )
        )

        // Workout 2: 04/11/2026 EMOM 20 min
        seedWorkout(
            workoutId = "home_gym_seed_emom_20260411",
            date = makeDate(2026, 4, 11),
            format = WorkoutFormat.EMOM,
            durationMinutes = 20,
            completedRounds = null,
            exercises = listOf(
                ExerciseSpec("custom_heavy_db_goblet_squat", reps = 8, weight = 52.5f),
                ExerciseSpec("custom_pull_up", reps = 6, weight = 0f),
                ExerciseSpec("custom_atomic_pushup_sliders", reps = 10, weight = 0f),
                ExerciseSpec("custom_ab_wheel_rollout", reps = 10, weight = 0f)
            )
        )

        // Recompute profile to include the new conditioning sessions
        profileComputerUseCase.recomputeFullProfile()
    }

    private suspend fun seedLmuBench20260412() {
        val lmuGym = gymRepository.getAllGyms().firstOrNull { it.name == "LMU Gym" } ?: return
        // Hevy seeder stores exercises under their Hevy name (not the mapped
        // app name), so look up "Bench Press (Barbell)" not "Barbell Bench Press"
        val exercise = exerciseRepository.getExerciseByName("Bench Press (Barbell)")
            ?: exerciseRepository.getExerciseByName("Barbell Bench Press")
            ?: return

        val date = Calendar.getInstance().apply {
            set(2026, 3, 12, 7, 0, 0) // April 12 2026, 7am
            set(Calendar.MILLISECOND, 0)
        }.time

        val workoutId = "lmu_seed_bench_20260412"
        val workout = Workout(
            id = workoutId,
            date = date,
            type = WorkoutType.PUSH,
            status = WorkoutStatus.COMPLETED,
            gymId = lmuGym.id,
            format = WorkoutFormat.STRENGTH,
            durationMinutes = 45
        )
        workoutRepository.createWorkout(workout)

        val sets = listOf(
            Set(reps = 12, weight = 135f, completed = true),
            Set(reps = 8, weight = 185f, completed = true),
            Set(reps = 5, weight = 195f, completed = true),
            Set(reps = 5, weight = 205f, completed = true),
            Set(reps = 3, weight = 215f, completed = true),
            Set(reps = 2, weight = 215f, completed = true),
            Set(reps = 2, weight = 225f, completed = true),
            Set(reps = 3, weight = 215f, completed = true),
            Set(reps = 3, weight = 205f, completed = true),
            Set(reps = 5, weight = 185f, completed = true),
            Set(reps = 12, weight = 135f, completed = true)
        )

        val workoutExercise = WorkoutExercise(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            exercise = exercise,
            sets = sets
        )
        workoutRepository.addExerciseToWorkout(workoutId, workoutExercise)

        profileComputerUseCase.recomputeFullProfile()
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

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
import kotlinx.coroutines.flow.first
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
        // Bump this to force all init flags to clear and re-run all steps.
        private const val INIT_VERSION = 3
        private const val KEY_INIT_VERSION = "init_version"

        // Floor used by the backup-restore sanity check. ExerciseDataV2
        // alone inserts ~94 rows; a healthy DB after KEY_BUNDLED_EXERCISES_SEEDED
        // has at least this many, so anything well below signals a
        // restored-prefs / empty-DB asymmetry.
        private const val MIN_EXERCISE_COUNT_FOR_HEALTHY_DB = 50

        private const val KEY_EXERCISES_INITIALIZED = "exercises_initialized"
        private const val KEY_EXERCISE_COUNT = "exercise_count"
        private const val KEY_BUNDLED_EXERCISES_SEEDED = "bundled_exercises_seeded"
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
        private const val KEY_HOME_GYM_EMOM_20260413_SEEDED = "home_gym_emom_20260413_seeded"
        private const val KEY_HOME_GYM_AMRAP_20260414_SEEDED = "home_gym_amrap_20260414_seeded"
        private const val KEY_LMU_PULL_20260414_SEEDED = "lmu_pull_20260414_seeded"
        private const val KEY_HOME_GYM_EMOM_20260415_SEEDED = "home_gym_emom_20260415_seeded"
        private const val KEY_HOME_GYM_POOL_EXPANSION_4_SEEDED = "home_gym_pool_expansion_4_seeded"
        private const val KEY_HOME_GYM_AMRAP_20260416_SEEDED = "home_gym_amrap_20260416_seeded"
        private const val KEY_HOME_GYM_POOL_EXPANSION_5_SEEDED = "home_gym_pool_expansion_5_seeded"
        private const val KEY_HOME_GYM_EMOM_20260417_SEEDED = "home_gym_emom_20260417_seeded"
        private const val KEY_EXERCISE_NAME_PRESCRIPTION_FIX = "exercise_name_prescription_fix"
        private const val KEY_HOME_GYM_POOL_EXPANSION_6_SEEDED = "home_gym_pool_expansion_6_seeded"
        private const val KEY_HOME_GYM_POOL_EXPANSION_7_SEEDED = "home_gym_pool_expansion_7_seeded"
        private const val KEY_HOME_GYM_EMOM_20260421_SEEDED = "home_gym_emom_20260421_seeded"
        private const val KEY_HOME_GYM_POOL_EXPANSION_8_SEEDED = "home_gym_pool_expansion_8_seeded"
        private const val KEY_HOME_GYM_EQUIPMENT_PLYO_BOX_ADDED = "home_gym_equipment_plyo_box_added"
        private const val KEY_HOME_GYM_POOL_EXPANSION_9_SEEDED = "home_gym_pool_expansion_9_seeded"
        private const val KEY_HOME_GYM_AMRAP_20260422_SEEDED = "home_gym_amrap_20260422_seeded"

        private val EXERCISE_NAME_FIX_IDS = setOf(
            "custom_row_200_400m",
            "custom_jump_rope_40_60s"
        )

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

        // Fourth pool expansion — punching bag (2) and slider (5) exercises.
        // Bag exercises go to CARDIO bucket (AMRAP only). Slider exercises
        // provide TRX-free alternatives across LOWER_BODY, CORE, and
        // CONDITIONING_BODYWEIGHT buckets.
        private val POOL_EXPANSION_4_IDS = setOf(
            "custom_bag_rounds",
            "custom_bag_combos",
            "custom_slider_reverse_lunge",
            "custom_slider_hamstring_curl",
            "custom_slider_mountain_climber",
            "custom_slider_pike",
            "custom_slider_body_saw"
        )

        // Fifth pool expansion — classic (bodyweight) clap push-up as a
        // non-TRX alternative in the UPPER_PUSH bucket. Needed by the
        // Apr 17 EMOM seed below; keeping it in its own expansion step
        // so the audit trail stays consistent with prior additions.
        private val POOL_EXPANSION_5_IDS = setOf(
            "custom_clap_pushup"
        )

        // Sixth pool expansion — 4 bodyweight push-up variants added from
        // user feedback. All UPPER_PUSH bucket, no equipment required.
        private val POOL_EXPANSION_6_IDS = setOf(
            "custom_decline_pushup",
            "custom_diamond_pushup",
            "custom_spiderman_pushup",
            "custom_single_leg_pushup"
        )

        // Seventh pool expansion — 9 bodyweight core additions from user
        // feedback. All CORE bucket, no equipment. Bodyweight "Russian Twist"
        // intentionally coexists alongside the existing Medicine Ball
        // variant as a no-equipment alternative.
        private val POOL_EXPANSION_7_IDS = setOf(
            "custom_bicycle_crunch",
            "custom_scissor_kicks",
            "custom_flutter_kicks",
            "custom_leg_raises",
            "custom_suitcase_crunch",
            "custom_v_sit",
            "custom_windshield_wipers",
            "custom_elevated_knee_crunch",
            "custom_russian_twist"
        )

        // Eighth pool expansion — 10 plyo box movements. Introduces a new
        // "Plyo Box" equipment tag; the companion migration
        // KEY_HOME_GYM_EQUIPMENT_PLYO_BOX_ADDED appends "Plyo Box" to the
        // Home Gym equipmentList so the generator can prescribe these.
        // Spans LOWER_BODY (4) and CONDITIONING_BODYWEIGHT (6) buckets.
        private val POOL_EXPANSION_8_IDS = setOf(
            "custom_step_up",
            "custom_bulgarian_split_squat",
            "custom_box_squat",
            "custom_single_leg_glute_bridge",
            "custom_box_jump",
            "custom_box_jump_over",
            "custom_step_up_jump",
            "custom_depth_jump",
            "custom_box_toe_taps",
            "custom_box_burpee"
        )

        // Ninth pool expansion — 11 calisthenics additions. Fills bodyweight
        // gaps across LOWER_BODY (wall sit, jump lunges, cossack squat),
        // UPPER_PULL (chin-up), UPPER_PUSH (archer, hindu, wide-grip,
        // tricep dip), CORE (hanging knee raise), and CONDITIONING_BODYWEIGHT
        // (bear crawl, inchworm). Chin-up and hanging knee raise use the
        // existing "Pull-Up Bar" tag; the rest are plain bodyweight.
        private val POOL_EXPANSION_9_IDS = setOf(
            "custom_wall_sit",
            "custom_jump_lunges",
            "custom_cossack_squat",
            "custom_chin_up",
            "custom_archer_pushup",
            "custom_hindu_pushup",
            "custom_wide_grip_pushup",
            "custom_tricep_dip",
            "custom_hanging_knee_raise",
            "custom_bear_crawl",
            "custom_inchworm"
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
            // Version gate: when INIT_VERSION bumps, clear all flags and
            // re-run every step. This recovers from any stale-flag state
            // (e.g., install-over where prefs persisted but DB is empty).
            if (prefs.getInt(KEY_INIT_VERSION, 0) < INIT_VERSION) {
                android.util.Log.w("InitExercises", "Init version bumped to $INIT_VERSION — clearing all flags")
                prefs.edit().clear().putInt(KEY_INIT_VERSION, INIT_VERSION).apply()
            }

            // Backup-restore defense: Android Auto Backup rehydrates
            // sharedprefs on install but not the Room DB (our backup_rules
            // only include the sharedpref domain). That leaves every
            // *_seeded flag=true while the exercises table is empty, and
            // every safeRun below takes the early-return path. Trust the
            // DB over the flags: if bundled_exercises claims seeded but
            // the table has fewer rows than ExerciseDataV2 alone inserts,
            // clear flags and re-seed from scratch.
            if (prefs.getBoolean(KEY_BUNDLED_EXERCISES_SEEDED, false) &&
                exerciseRepository.countExercises() < MIN_EXERCISE_COUNT_FOR_HEALTHY_DB
            ) {
                android.util.Log.w(
                    "InitExercises",
                    "Seed flags set but DB has ${exerciseRepository.countExercises()} exercises — clearing flags to re-seed"
                )
                prefs.edit().clear().putInt(KEY_INIT_VERSION, INIT_VERSION).apply()
            }

            // All exercises are bundled in ExerciseDataV2 and the various
            // catalogs. Each step is gated by its own SharedPreferences flag
            // and wrapped in safeRun so partial failures don't block others.
            safeRun(KEY_BUNDLED_EXERCISES_SEEDED) {
                val domainExercises = com.workoutapp.data.database.ExerciseDataV2.exercises.map { entity ->
                    com.workoutapp.domain.model.Exercise(
                        id = entity.id,
                        name = entity.name,
                        muscleGroups = entity.muscleGroups,
                        equipment = entity.equipment,
                        category = entity.category,
                        imageUrl = entity.imageUrl,
                        instructions = entity.instructions,
                        isUserCreated = entity.isUserCreated
                    )
                }
                exerciseRepository.insertExercises(domainExercises)
            }

            safeRun(KEY_CATEGORY_BACKFILLED) {
                exerciseRepository.backfillExerciseCategories()
            }

            // Phase 3: one-time seed of the original custom conditioning exercises
            // (TRX, medicine ball, ab wheel, cardio stations). Most of these are
            // superseded by the Phase 3.1 catalog below, but we still insert them
            // so IDs stay stable for any historical workouts that reference them.
            safeRun(KEY_CONDITIONING_EXERCISES_SEEDED) {
                exerciseRepository.insertExercises(CustomExerciseSeeder.exercises)
            }

            // Phase 3.1 fix 1: reclassify cardio/plyometric exercises that were
            // previously miscategorized as STRENGTH_LEGS or similar due to their
            // muscle group tags (e.g., "Fast Skipping" showing up on pull day).
            // Runs once per install regardless of the backfill flag.
            safeRun(KEY_CARDIO_NAMES_RECLASSIFIED) {
                exerciseRepository.reclassifyCardioByName()
            }

            safeRun(KEY_HOME_GYM_CATALOG_SEEDED) {
                val catalogExercises = HomeGymCatalogSeeder.buildExercises()
                exerciseRepository.insertExercises(catalogExercises)
                val catalogIds = HomeGymMovementCatalog.movements.map { it.id }
                val phase3OrphanIds = CustomExerciseSeeder.exercises.map { it.id }
                val allIds = (catalogIds + phase3OrphanIds).distinct()
                exerciseRepository.setUserExercises(allIds)
            }

            safeRun(KEY_HOME_GYM_UPPER_EXPANSION_SEEDED) {
                val newExercises = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in UPPER_EXPANSION_IDS }
                exerciseRepository.insertExercises(newExercises)
                exerciseRepository.setUserExercises(UPPER_EXPANSION_IDS.toList())
            }

            safeRun(KEY_HOME_GYM_POOL_EXPANSION_2_SEEDED) {
                val newExercises = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in POOL_EXPANSION_2_IDS }
                exerciseRepository.insertExercises(newExercises)
                exerciseRepository.setUserExercises(POOL_EXPANSION_2_IDS.toList())
            }

            safeRun(KEY_HOME_GYM_POOL_EXPANSION_3_SEEDED) {
                val newExercises = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in POOL_EXPANSION_3_IDS }
                exerciseRepository.insertExercises(newExercises)
                exerciseRepository.setUserExercises(POOL_EXPANSION_3_IDS.toList())
            }

            safeRun(KEY_LMU_LEG_CATALOG_SEEDED) {
                val newLmuLegs = LmuLegCatalog.buildNewExercises()
                exerciseRepository.insertExercises(newLmuLegs)
                exerciseRepository.setUserExercises(LmuLegCatalog.allowedIds.toList())
            }

            safeRun(KEY_HEVY_HISTORY_SEEDED) {
                val lmuGym = gymRepository.getAllGyms().firstOrNull { it.name == "Gym" }
                hevyHistorySeeder.seedFromBundledCsv(gymId = lmuGym?.id)
            }

            userPreferencesRepository.markOnboardingComplete()

            safeRun(KEY_GYM_DEDUP_APPLIED) {
                deduplicateGyms()
            }

            safeRun(KEY_LMU_BODYWEIGHT_REMOVED) {
                val lmuGymForEquip = gymRepository.getAllGyms().firstOrNull { it.name == "Gym" }
                if (lmuGymForEquip != null) {
                    val filtered = lmuGymForEquip.equipmentList.filterNot { it in listOf("Bodyweight", "None") }
                    gymRepository.updateGym(lmuGymForEquip.copy(equipmentList = filtered))
                }
            }

            safeRun(KEY_EXERCISEDB_ACTIVATED) {
                val exerciseDbIds = ExerciseDataV2.exercises.map { it.id }
                exerciseRepository.setUserExercises(exerciseDbIds)
            }

            safeRun(KEY_HOME_GYM_HISTORY_SEEDED) {
                seedHomeGymHistory()
            }

            safeRun(KEY_LMU_BENCH_20260412_SEEDED) {
                seedLmuBench20260412()
            }

            safeRun(KEY_HOME_GYM_EMOM_20260413_SEEDED) {
                seedHomeGymEmom20260413()
            }

            safeRun(KEY_HOME_GYM_AMRAP_20260414_SEEDED) {
                seedHomeGymAmrap20260414()
            }

            safeRun(KEY_LMU_PULL_20260414_SEEDED) {
                seedLmuPull20260414()
            }

            safeRun(KEY_HOME_GYM_POOL_EXPANSION_4_SEEDED) {
                val newExercises = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in POOL_EXPANSION_4_IDS }
                exerciseRepository.insertExercises(newExercises)
                exerciseRepository.setUserExercises(POOL_EXPANSION_4_IDS.toList())
            }

            safeRun(KEY_HOME_GYM_EMOM_20260415_SEEDED) {
                seedHomeGymEmom20260415()
            }

            safeRun(KEY_HOME_GYM_AMRAP_20260416_SEEDED) {
                seedHomeGymAmrap20260416()
            }

            safeRun(KEY_HOME_GYM_POOL_EXPANSION_5_SEEDED) {
                val newExercises = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in POOL_EXPANSION_5_IDS }
                exerciseRepository.insertExercises(newExercises)
                exerciseRepository.setUserExercises(POOL_EXPANSION_5_IDS.toList())
            }

            safeRun(KEY_HOME_GYM_EMOM_20260417_SEEDED) {
                seedHomeGymEmom20260417()
            }

            safeRun(KEY_HOME_GYM_POOL_EXPANSION_6_SEEDED) {
                val newExercises = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in POOL_EXPANSION_6_IDS }
                exerciseRepository.insertExercises(newExercises)
                exerciseRepository.setUserExercises(POOL_EXPANSION_6_IDS.toList())
            }

            safeRun(KEY_HOME_GYM_POOL_EXPANSION_7_SEEDED) {
                val newExercises = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in POOL_EXPANSION_7_IDS }
                exerciseRepository.insertExercises(newExercises)
                exerciseRepository.setUserExercises(POOL_EXPANSION_7_IDS.toList())
            }

            safeRun(KEY_HOME_GYM_EMOM_20260421_SEEDED) {
                seedHomeGymEmom20260421()
            }

            safeRun(KEY_HOME_GYM_POOL_EXPANSION_8_SEEDED) {
                val newExercises = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in POOL_EXPANSION_8_IDS }
                exerciseRepository.insertExercises(newExercises)
                exerciseRepository.setUserExercises(POOL_EXPANSION_8_IDS.toList())
            }

            safeRun(KEY_HOME_GYM_EQUIPMENT_PLYO_BOX_ADDED) {
                val homeGym = gymRepository.getAllGyms().firstOrNull { it.name == "Home Gym" }
                if (homeGym != null && "Plyo Box" !in homeGym.equipmentList) {
                    val updated = homeGym.equipmentList + "Plyo Box"
                    gymRepository.updateGym(homeGym.copy(equipmentList = updated))
                }
            }

            safeRun(KEY_HOME_GYM_POOL_EXPANSION_9_SEEDED) {
                val newExercises = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in POOL_EXPANSION_9_IDS }
                exerciseRepository.insertExercises(newExercises)
                exerciseRepository.setUserExercises(POOL_EXPANSION_9_IDS.toList())
            }

            safeRun(KEY_EXERCISE_NAME_PRESCRIPTION_FIX) {
                val fixes = HomeGymCatalogSeeder.buildExercises()
                    .filter { it.id in EXERCISE_NAME_FIX_IDS }
                exerciseRepository.insertExercises(fixes)
            }

            safeRun(KEY_HOME_GYM_AMRAP_20260422_SEEDED) {
                seedHomeGymAmrap20260422()
            }

            Result.success(null)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Run a seeding step independently — a failure in one step must not
     * prevent subsequent steps from running. Each step is gated by a
     * SharedPreferences flag that is set only on success.
     */
    private suspend fun safeRun(key: String, block: suspend () -> Unit) {
        if (prefs.getBoolean(key, false)) return
        try {
            block()
            prefs.edit().putBoolean(key, true).apply()
        } catch (t: Throwable) {
            android.util.Log.e("InitExercises", "Step $key failed: ${t.message}", t)
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
        val lmuGym = gymRepository.getAllGyms().firstOrNull { it.name == "Gym" } ?: return
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

    private suspend fun seedHomeGymEmom20260413() {
        val homeGym = gymRepository.getAllGyms().firstOrNull { it.name == "Home Gym" } ?: return

        val date = Calendar.getInstance().apply {
            set(2026, 3, 13, 6, 0, 0) // April 13 2026, 6am
            set(Calendar.MILLISECOND, 0)
        }.time

        val exerciseIds = listOf(
            "custom_kb_swing",
            "custom_trx_single_arm_row",
            "custom_atomic_pushup_trx",
            "custom_trx_mountain_climber"
        )
        val resolved = exerciseIds.mapNotNull { exerciseRepository.getExerciseById(it) }
        if (resolved.size != exerciseIds.size) return

        val prescriptions = listOf("\u00d7 10", "\u00d7 6/side", "\u00d7 10", "\u00d7 40")

        val workoutId = "home_gym_seed_emom_20260413"
        val workout = Workout(
            id = workoutId,
            date = date,
            type = WorkoutType.PULL,
            status = WorkoutStatus.COMPLETED,
            gymId = homeGym.id,
            format = WorkoutFormat.EMOM,
            durationMinutes = 20,
            completedRounds = 5
        )
        workoutRepository.createWorkout(workout)

        resolved.forEachIndexed { index, exercise ->
            val we = WorkoutExercise(
                id = UUID.randomUUID().toString(),
                workoutId = workoutId,
                exercise = exercise,
                sets = listOf(Set(reps = 0, weight = 0f, completed = true)),
                prescription = prescriptions[index]
            )
            workoutRepository.addExerciseToWorkout(workoutId, we)
        }

        profileComputerUseCase.recomputeFullProfile()
    }

    private suspend fun seedHomeGymAmrap20260414() {
        val homeGym = gymRepository.getAllGyms().firstOrNull { it.name == "Home Gym" } ?: return

        val date = Calendar.getInstance().apply {
            set(2026, 3, 14, 5, 45, 0) // April 14 2026, 5:45am
            set(Calendar.MILLISECOND, 0)
        }.time

        val exerciseIds = listOf(
            "custom_burpees",
            "custom_trx_side_lunge",
            "custom_kb_clean"
        )
        val resolved = exerciseIds.mapNotNull { exerciseRepository.getExerciseById(it) }
        if (resolved.size != exerciseIds.size) return

        val prescriptions = listOf("\u00d7 6-8", "\u00d7 5-6/side", "\u00d7 6/side @ 20lb")

        val workoutId = "home_gym_seed_amrap_20260414"
        val workout = Workout(
            id = workoutId,
            date = date,
            type = WorkoutType.PULL,
            status = WorkoutStatus.COMPLETED,
            gymId = homeGym.id,
            format = WorkoutFormat.AMRAP,
            durationMinutes = 15,
            completedRounds = 9
        )
        workoutRepository.createWorkout(workout)

        resolved.forEachIndexed { index, exercise ->
            val we = WorkoutExercise(
                id = UUID.randomUUID().toString(),
                workoutId = workoutId,
                exercise = exercise,
                sets = listOf(Set(reps = 0, weight = 0f, completed = true)),
                prescription = prescriptions[index]
            )
            workoutRepository.addExerciseToWorkout(workoutId, we)
        }

        profileComputerUseCase.recomputeFullProfile()
    }

    /**
     * Apr 14 2026 LMU pull session recovered from memory after the app lost
     * the in-progress workout when backgrounded (root cause fixed on this
     * branch via autosave + initial-persist). Marc logs real sessions into
     * the seed as durable history; this appends one more entry.
     *
     * Weights are logged per the literal values Marc stated: DB exercises
     * use per-hand weight (matches Hevy convention for bilateral DB lifts
     * and his mental model); the plate-loaded machine (Iso-Lateral High
     * Row) uses total plate weight.
     */
    private suspend fun seedLmuPull20260414() {
        val lmuGym = gymRepository.getAllGyms().firstOrNull { it.name == "Gym" } ?: return

        // Hevy name lookups (Hevy seeder stores under Hevy names, not mapped app names).
        val squatCurl = exerciseRepository.getExerciseByName("Squat Curl")
            ?: exerciseRepository.getExerciseByName("Squat Curl (DB)")
            ?: return
        val isoHighRow = exerciseRepository.getExerciseByName("Iso-Lateral High Row (Machine)")
            ?: return
        val hammerCurl = exerciseRepository.getExerciseByName("Hammer Curl (Dumbbell)")
            ?: exerciseRepository.getExerciseByName("Hammer Curl")
            ?: return

        val date = Calendar.getInstance().apply {
            set(2026, 3, 14, 7, 0, 0) // April 14 2026, 7am
            set(Calendar.MILLISECOND, 0)
        }.time

        val workoutId = "lmu_seed_pull_20260414"
        val workout = Workout(
            id = workoutId,
            date = date,
            type = WorkoutType.PULL,
            status = WorkoutStatus.COMPLETED,
            gymId = lmuGym.id,
            format = WorkoutFormat.STRENGTH,
            durationMinutes = 45
        )
        workoutRepository.createWorkout(workout)

        val squatCurlWe = WorkoutExercise(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            exercise = squatCurl,
            sets = listOf(
                Set(reps = 10, weight = 40f, completed = true),
                Set(reps = 10, weight = 40f, completed = true),
                Set(reps = 10, weight = 40f, completed = true)
            )
        )
        val isoHighRowWe = WorkoutExercise(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            exercise = isoHighRow,
            sets = listOf(
                Set(reps = 10, weight = 174f, completed = true),
                Set(reps = 10, weight = 184f, completed = true),
                Set(reps = 10, weight = 194f, completed = true)
            )
        )
        val hammerCurlWe = WorkoutExercise(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            exercise = hammerCurl,
            sets = listOf(
                Set(reps = 10, weight = 30f, completed = true),
                Set(reps = 10, weight = 30f, completed = true)
            )
        )

        workoutRepository.addExerciseToWorkout(workoutId, squatCurlWe)
        workoutRepository.addExerciseToWorkout(workoutId, isoHighRowWe)
        workoutRepository.addExerciseToWorkout(workoutId, hammerCurlWe)

        profileComputerUseCase.recomputeFullProfile()
    }

    private suspend fun seedHomeGymEmom20260415() {
        val homeGym = gymRepository.getAllGyms().firstOrNull { it.name == "Home Gym" } ?: return

        val date = Calendar.getInstance().apply {
            set(2026, 3, 15, 6, 0, 0) // April 15 2026, 6am
            set(Calendar.MILLISECOND, 0)
        }.time

        val exerciseIds = listOf(
            "custom_trx_hamstring_curl",
            "custom_pull_up",
            "custom_pair_db_push_press",
            "custom_plank"
        )
        val resolved = exerciseIds.mapNotNull { exerciseRepository.getExerciseById(it) }
        if (resolved.size != exerciseIds.size) return

        val prescriptions = listOf("\u00d7 10-15", "\u00d7 5-6", "\u00d7 8-10", "30-40 sec")

        val workoutId = "home_gym_seed_emom_20260415"
        val workout = Workout(
            id = workoutId,
            date = date,
            type = WorkoutType.PULL,
            status = WorkoutStatus.COMPLETED,
            gymId = homeGym.id,
            format = WorkoutFormat.EMOM,
            durationMinutes = 20,
            completedRounds = 5
        )
        workoutRepository.createWorkout(workout)

        resolved.forEachIndexed { index, exercise ->
            val we = WorkoutExercise(
                id = UUID.randomUUID().toString(),
                workoutId = workoutId,
                exercise = exercise,
                sets = listOf(Set(reps = 0, weight = 0f, completed = true)),
                prescription = prescriptions[index]
            )
            workoutRepository.addExerciseToWorkout(workoutId, we)
        }

        profileComputerUseCase.recomputeFullProfile()
    }

    private suspend fun seedHomeGymEmom20260417() {
        val homeGym = gymRepository.getAllGyms().firstOrNull { it.name == "Home Gym" } ?: return

        val date = Calendar.getInstance().apply {
            set(2026, 3, 17, 5, 30, 0) // April 17 2026, 5:30am
            set(Calendar.MILLISECOND, 0)
        }.time

        val exerciseIds = listOf(
            "custom_jump_squats",
            "custom_clap_pushup",
            "custom_trx_pistol_squat",
            "custom_ab_wheel_rollout"
        )
        val resolved = exerciseIds.mapNotNull { exerciseRepository.getExerciseById(it) }
        if (resolved.size != exerciseIds.size) return

        val prescriptions = listOf("\u00d7 10", "\u00d7 10", "\u00d7 6/side", "\u00d7 10")

        val workoutId = "home_gym_seed_emom_20260417"
        val workout = Workout(
            id = workoutId,
            date = date,
            type = WorkoutType.PULL,
            status = WorkoutStatus.COMPLETED,
            gymId = homeGym.id,
            format = WorkoutFormat.EMOM,
            durationMinutes = 20,
            completedRounds = 5
        )
        workoutRepository.createWorkout(workout)

        resolved.forEachIndexed { index, exercise ->
            val we = WorkoutExercise(
                id = UUID.randomUUID().toString(),
                workoutId = workoutId,
                exercise = exercise,
                sets = listOf(Set(reps = 0, weight = 0f, completed = true)),
                prescription = prescriptions[index]
            )
            workoutRepository.addExerciseToWorkout(workoutId, we)
        }

        profileComputerUseCase.recomputeFullProfile()
    }

    private suspend fun seedHomeGymAmrap20260416() {
        val homeGym = gymRepository.getAllGyms().firstOrNull { it.name == "Home Gym" } ?: return

        val date = Calendar.getInstance().apply {
            set(2026, 3, 16, 5, 20, 0) // April 16 2026, 5:20am
            set(Calendar.MILLISECOND, 0)
        }.time

        val exerciseIds = listOf(
            "custom_row_200_400m",
            "custom_atomic_pushup_sliders",
            "custom_trx_crunch"
        )
        val resolved = exerciseIds.mapNotNull { exerciseRepository.getExerciseById(it) }
        if (resolved.size != exerciseIds.size) return

        val prescriptions = listOf("200m row", "\u00d7 8-12", "\u00d7 12-15")

        val workoutId = "home_gym_seed_amrap_20260416"
        val workout = Workout(
            id = workoutId,
            date = date,
            type = WorkoutType.PULL,
            status = WorkoutStatus.COMPLETED,
            gymId = homeGym.id,
            format = WorkoutFormat.AMRAP,
            durationMinutes = 15,
            completedRounds = 4
        )
        workoutRepository.createWorkout(workout)

        resolved.forEachIndexed { index, exercise ->
            val we = WorkoutExercise(
                id = UUID.randomUUID().toString(),
                workoutId = workoutId,
                exercise = exercise,
                sets = listOf(Set(reps = 0, weight = 0f, completed = true)),
                prescription = prescriptions[index]
            )
            workoutRepository.addExerciseToWorkout(workoutId, we)
        }

        profileComputerUseCase.recomputeFullProfile()
    }

    private suspend fun seedHomeGymEmom20260421() {
        val homeGym = gymRepository.getAllGyms().firstOrNull { it.name == "Home Gym" } ?: return

        val date = Calendar.getInstance().apply {
            set(2026, 3, 21, 5, 30, 0) // April 21 2026, 5:30am
            set(Calendar.MILLISECOND, 0)
        }.time

        val exerciseIds = listOf(
            "custom_heavy_db_goblet_squat",
            "custom_pull_up",
            "custom_heavy_db_push_press",
            "custom_mountain_climbers"
        )
        val resolved = exerciseIds.mapNotNull { exerciseRepository.getExerciseById(it) }
        if (resolved.size != exerciseIds.size) return

        val prescriptions = listOf("× 8", "× 6", "× 6/side", "× 20")

        val workoutId = "home_gym_seed_emom_20260421"
        val workout = Workout(
            id = workoutId,
            date = date,
            type = WorkoutType.PULL,
            status = WorkoutStatus.COMPLETED,
            gymId = homeGym.id,
            format = WorkoutFormat.EMOM,
            durationMinutes = 20,
            completedRounds = 5
        )
        workoutRepository.createWorkout(workout)

        resolved.forEachIndexed { index, exercise ->
            val we = WorkoutExercise(
                id = UUID.randomUUID().toString(),
                workoutId = workoutId,
                exercise = exercise,
                sets = listOf(Set(reps = 0, weight = 0f, completed = true)),
                prescription = prescriptions[index]
            )
            workoutRepository.addExerciseToWorkout(workoutId, we)
        }

        profileComputerUseCase.recomputeFullProfile()
    }

    /**
     * Seeds the 2026-04-22 Home Gym AMRAP (15-min, 5 rounds: Bag Rounds +
     * Pair DB Front Squat + Plank). Unlike the 04/21 EMOM which recovered
     * an unsaved live session, this AMRAP *was* completed live — the seed
     * exists to survive a forced uninstall (`allowBackup="false"` wipes
     * workout history). Idempotent against the live row: if an AMRAP
     * workout already exists on 04/22 at Home Gym, we skip insertion so
     * current devices don't end up with a visible duplicate in Pack History.
     */
    private suspend fun seedHomeGymAmrap20260422() {
        val homeGym = gymRepository.getAllGyms().firstOrNull { it.name == "Home Gym" } ?: return

        val date = Calendar.getInstance().apply {
            set(2026, 3, 22, 5, 0, 0) // April 22 2026, 5:00am
            set(Calendar.MILLISECOND, 0)
        }.time

        // Skip if the live AMRAP on this date is already persisted. Prevents
        // a user-visible duplicate Pack History row on existing installs. On
        // a fresh install (post-uninstall), this query returns empty and the
        // seed runs as intended.
        val existingAmrap = workoutRepository
            .getConditioningWorkoutsInMonth(homeGym.id)
            .firstOrNull { it.format == WorkoutFormat.AMRAP && isSameDay(it.date, date) }
        if (existingAmrap != null) return

        val exerciseIds = listOf(
            "custom_bag_rounds",
            "custom_pair_db_squat",
            "custom_plank"
        )
        val resolved = exerciseIds.mapNotNull { exerciseRepository.getExerciseById(it) }
        if (resolved.size != exerciseIds.size) return

        val prescriptions = listOf("40s freestyle", "× 6-8", "20-30 sec")

        val workoutId = "home_gym_seed_amrap_20260422"
        val workout = Workout(
            id = workoutId,
            date = date,
            type = WorkoutType.PULL,
            status = WorkoutStatus.COMPLETED,
            gymId = homeGym.id,
            format = WorkoutFormat.AMRAP,
            durationMinutes = 15,
            completedRounds = 5
        )
        workoutRepository.createWorkout(workout)

        resolved.forEachIndexed { index, exercise ->
            val we = WorkoutExercise(
                id = UUID.randomUUID().toString(),
                workoutId = workoutId,
                exercise = exercise,
                sets = listOf(Set(reps = 0, weight = 0f, completed = true)),
                prescription = prescriptions[index]
            )
            workoutRepository.addExerciseToWorkout(workoutId, we)
        }

        profileComputerUseCase.recomputeFullProfile()
    }

    private fun isSameDay(a: java.util.Date, b: java.util.Date): Boolean {
        val ca = Calendar.getInstance().apply { time = a }
        val cb = Calendar.getInstance().apply { time = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
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

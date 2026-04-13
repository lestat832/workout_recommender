package com.workoutapp.domain.usecase

import com.workoutapp.data.database.LmuLegCatalog
import com.workoutapp.domain.model.EquipmentType
import com.workoutapp.domain.model.ExerciseCategory
import com.workoutapp.domain.model.GeneratedWorkout
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.GymRepository
import com.workoutapp.domain.repository.TrainingProfileRepository
import com.workoutapp.domain.repository.WorkoutRepository
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

class GenerateWorkoutUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val gymRepository: GymRepository,
    private val profileRepository: TrainingProfileRepository
) {
    suspend operator fun invoke(
        gymId: Long? = null,
        typeOverride: WorkoutType? = null
    ): GeneratedWorkout {
        // Load the target gym for equipment filtering. If a gymId is provided
        // use it directly; otherwise fall back to the default gym to preserve
        // pre-Phase-1 callers.
        val gym = gymId?.let { gymRepository.getGymById(it) } ?: gymRepository.getDefaultGym()

        // Skip-button flow: when the caller forces a type (user tapped skip on
        // the NextWorkoutCard), bypass the week-reset + alternation resolver.
        // Override applies to this session only; next completion recomputes.
        val workoutType = typeOverride ?: resolveType(gymId)

        // Recent exercise IDs to avoid (7-day cooldown, completed workouts only
        // per Phase 0).
        val recentExerciseIds = workoutRepository.getExerciseIdsFromLastWeek()

        // Filter by ExerciseCategory (Phase 0 taxonomy) instead of the legacy
        // Exercise.category: WorkoutType field. PULL pulls from STRENGTH_PULL
        // union STRENGTH_LEGS so quads/calves/glutes finally surface on pull
        // day; they were miscategorized as PUSH in the legacy field.
        val categories = categoriesFor(workoutType)
        val availableExercises = exerciseRepository.getUserActiveExercisesByCategories(categories)
            .filter { exercise ->
                gym?.let { g ->
                    EquipmentType.canPerformExercise(exercise.equipment, g.equipmentList)
                } ?: true
            }
            .filterNot { it.id in recentExerciseIds }

        val exercisesByMuscle = availableExercises
            .groupBy { it.muscleGroups.firstOrNull() ?: MuscleGroup.CHEST }

        val targetMuscleGroups = if (workoutType == WorkoutType.PUSH) {
            listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDER, MuscleGroup.TRICEP)
        } else {
            listOf(MuscleGroup.LEGS, MuscleGroup.BACK, MuscleGroup.BICEP)
        }

        // Load exercise profiles for plateau detection
        val allCandidateIds = availableExercises.map { it.id }
        val exerciseProfiles = profileRepository.getExerciseProfiles(allCandidateIds)
            .associateBy { it.exerciseId }

        val selected = targetMuscleGroups.mapNotNull { muscleGroup ->
            val candidates = exercisesByMuscle[muscleGroup] ?: return@mapNotNull null
            val filtered = when (muscleGroup) {
                MuscleGroup.LEGS -> candidates
                    .filter { it.id in LmuLegCatalog.allowedIds }
                    .ifEmpty { candidates }
                else -> candidates
            }
            // Deprioritize plateau'd exercises: prefer non-plateau'd, fall back to all
            val nonPlateau = filtered.filter { exercise ->
                val profile = exerciseProfiles[exercise.id]
                profile == null || !profile.plateauFlag
            }
            val pool = nonPlateau.ifEmpty { filtered }
            pool.randomOrNull()
        }.take(3)

        return GeneratedWorkout(type = workoutType, exercises = selected)
    }

    /**
     * Lightweight type prediction for HomeScreen's NextWorkoutCard. Performs a
     * single DB read plus date math, no exercise filtering. Safe to call on
     * every gym change.
     */
    suspend fun predictNextType(gymId: Long): WorkoutType = resolveType(gymId)

    /**
     * Resolves the next workout type for a given gym.
     *
     * Rules:
     *  - No gymId (legacy/defensive path): plain global alternation, no week reset
     *  - No completed workout at this gym: PULL (first-ever workout)
     *  - Last completed workout at this gym is before this week's Monday midnight:
     *    PULL (week reset — first workout of a new week is always PULL)
     *  - Otherwise: alternate from the last completed workout's type
     */
    private suspend fun resolveType(gymId: Long?): WorkoutType {
        if (gymId == null) {
            val last = workoutRepository.getLastWorkout()
            return if (last?.type == WorkoutType.PUSH) WorkoutType.PULL else WorkoutType.PUSH
        }
        val last = workoutRepository.getLastCompletedWorkoutByGym(gymId)
        val weekStart = currentWeekStartMonday()
        val baseType = when {
            last == null -> WorkoutType.PULL
            last.date.before(weekStart) -> WorkoutType.PULL
            last.type == WorkoutType.PUSH -> WorkoutType.PULL
            else -> WorkoutType.PUSH
        }

        // Push/pull ratio nudge: if push-heavy (>1.8 ratio) and alternation
        // says PUSH, override to PULL to rebalance. Only applies when the
        // profile has enough data to be meaningful.
        if (baseType == WorkoutType.PUSH) {
            val globalProfile = profileRepository.getGlobalProfile()
            if (globalProfile != null && globalProfile.totalStrengthSessions >= 10 && globalProfile.pushPullRatio > 1.8f) {
                return WorkoutType.PULL
            }
        }

        return baseType
    }

    private fun categoriesFor(type: WorkoutType): List<ExerciseCategory> = when (type) {
        WorkoutType.PUSH -> listOf(ExerciseCategory.STRENGTH_PUSH)
        WorkoutType.PULL -> listOf(ExerciseCategory.STRENGTH_PULL, ExerciseCategory.STRENGTH_LEGS)
    }

    /**
     * Returns Monday 00:00 local time of the current calendar week. Used to
     * detect whether the last completed workout belongs to a previous week.
     */
    private fun currentWeekStartMonday(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val daysFromMonday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        return cal.time
    }
}

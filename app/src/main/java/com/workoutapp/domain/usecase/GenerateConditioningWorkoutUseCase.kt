package com.workoutapp.domain.usecase

import com.workoutapp.domain.model.EquipmentType
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.ExerciseCategory
import com.workoutapp.domain.model.GeneratedConditioningWorkout
import com.workoutapp.domain.model.Gym
import com.workoutapp.domain.model.WorkoutFormat
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.GymRepository
import com.workoutapp.domain.repository.WorkoutRepository
import javax.inject.Inject
import kotlin.random.Random

/**
 * Generates a Home Gym conditioning workout: a randomly chosen EMOM or AMRAP
 * format, 20 minutes, composed of four exercises — one push, one pull, one
 * legs-or-core, and one cardio station (rower/bike/jump rope).
 *
 * Monthly de-duplication: the resulting exercise set is fingerprinted and
 * compared against any conditioning workout already completed at this gym
 * this calendar month. On match, up to 5 retries pick a fresh candidate.
 * After exhaustion, the last candidate ships (fallback).
 */
class GenerateConditioningWorkoutUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val gymRepository: GymRepository
) {
    companion object {
        private const val DURATION_MINUTES = 20
        private const val MAX_RETRIES = 5
    }

    suspend operator fun invoke(gymId: Long): GeneratedConditioningWorkout {
        val gym = gymRepository.getGymById(gymId)
            ?: throw IllegalStateException("Gym not found: $gymId")

        val pool = loadPool(gym)
        val existing = workoutRepository
            .getConditioningWorkoutsInMonth(gymId)
            .map { fingerprint(it.exercises.map { e -> e.exercise.id }) }
            .toSet()

        var candidate = sampleCandidate(pool)
            ?: throw IllegalStateException(
                "No exercises available for conditioning workout at gym $gymId"
            )

        var attempts = 0
        while (fingerprint(candidate.map { it.id }) in existing && attempts < MAX_RETRIES) {
            val next = sampleCandidate(pool) ?: break
            candidate = next
            attempts++
        }

        val format = if (Random.nextBoolean()) WorkoutFormat.EMOM else WorkoutFormat.AMRAP

        return GeneratedConditioningWorkout(
            format = format,
            exercises = candidate,
            durationMinutes = DURATION_MINUTES
        )
    }

    private suspend fun loadPool(gym: Gym): List<Exercise> {
        val categories = listOf(
            ExerciseCategory.STRENGTH_PUSH,
            ExerciseCategory.STRENGTH_PULL,
            ExerciseCategory.STRENGTH_LEGS,
            ExerciseCategory.CORE,
            ExerciseCategory.CARDIO_CONDITIONING
        )
        return exerciseRepository.getUserActiveExercisesByCategories(categories)
            .filter { exercise ->
                EquipmentType.canPerformExercise(exercise.equipment, gym.equipmentList)
            }
    }

    private fun sampleCandidate(pool: List<Exercise>): List<Exercise>? {
        val push = pool.filter {
            it.exerciseCategory == ExerciseCategory.STRENGTH_PUSH
        }.randomOrNull() ?: return null

        val pull = pool.filter {
            it.exerciseCategory == ExerciseCategory.STRENGTH_PULL
        }.randomOrNull() ?: return null

        val legsOrCore = pool.filter {
            it.exerciseCategory == ExerciseCategory.STRENGTH_LEGS ||
                it.exerciseCategory == ExerciseCategory.CORE
        }.randomOrNull() ?: return null

        val cardio = pool.filter {
            it.exerciseCategory == ExerciseCategory.CARDIO_CONDITIONING
        }.randomOrNull() ?: return null

        return listOf(push, pull, legsOrCore, cardio)
    }

    private fun fingerprint(ids: List<String>): String = ids.sorted().joinToString(",")
}

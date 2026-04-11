package com.workoutapp.domain.usecase

import com.workoutapp.data.database.HomeGymMovementCatalog
import com.workoutapp.data.database.HomeGymMovementCatalog.Bucket
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.GeneratedConditioningWorkout
import com.workoutapp.domain.model.WorkoutFormat
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.WorkoutRepository
import javax.inject.Inject
import kotlin.random.Random

/**
 * Generates a Home Gym conditioning workout from the hand-curated
 * [HomeGymMovementCatalog]. Picks an EMOM or AMRAP format and composes
 * 3 or 4 movements according to the builder rules:
 *
 *  - AMRAP (20 min): always 3 movements = cardio + strength + core-or-conditioning
 *  - EMOM 3-station (default, 60%): same structure as AMRAP
 *  - EMOM 4-station (balanced, 40%): cardio + lower body + upper body + core-or-conditioning
 *
 * Monthly de-duplication fingerprints the sorted movement ids and compares
 * against any conditioning workout completed at this gym this calendar month.
 * Up to [MAX_RETRIES] resamples on match; falls back to the last candidate
 * after exhaustion.
 */
class GenerateConditioningWorkoutUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository
) {
    companion object {
        private const val DURATION_MINUTES = 20
        private const val MAX_RETRIES = 5
        private const val EMOM_FOUR_STATION_PROBABILITY = 0.4
    }

    suspend operator fun invoke(gymId: Long): GeneratedConditioningWorkout {
        val format = if (Random.nextBoolean()) WorkoutFormat.EMOM else WorkoutFormat.AMRAP
        val stationCount = when (format) {
            WorkoutFormat.AMRAP -> 3
            WorkoutFormat.EMOM -> if (Random.nextDouble() < EMOM_FOUR_STATION_PROBABILITY) 4 else 3
            else -> 3
        }

        val existingFingerprints = workoutRepository
            .getConditioningWorkoutsInMonth(gymId)
            .map { fingerprint(it.exercises.map { e -> e.exercise.id }) }
            .toSet()

        var candidateIds = pickMovementIds(stationCount)
        var attempts = 0
        while (fingerprint(candidateIds) in existingFingerprints && attempts < MAX_RETRIES) {
            candidateIds = pickMovementIds(stationCount)
            attempts++
        }

        val exercises = candidateIds.mapNotNull { exerciseRepository.getExerciseById(it) }
        if (exercises.size < stationCount) {
            throw IllegalStateException(
                "Home gym catalog incomplete: expected $stationCount exercises, " +
                    "found ${exercises.size}. Check HomeGymCatalogSeeder ran on install."
            )
        }

        return GeneratedConditioningWorkout(
            format = format,
            exercises = exercises,
            durationMinutes = DURATION_MINUTES
        )
    }

    /**
     * Picks movement ids from the catalog according to the builder rules for
     * the given station count. Returns a list of stable catalog ids.
     */
    private fun pickMovementIds(stationCount: Int): List<String> {
        val byBucket = HomeGymMovementCatalog.byBucket()
        val cardio = byBucket[Bucket.CARDIO].orEmpty()
        val lower = byBucket[Bucket.LOWER_BODY].orEmpty()
        val upper = byBucket[Bucket.UPPER_BODY].orEmpty()
        val core = byBucket[Bucket.CORE].orEmpty()
        val conditioning = byBucket[Bucket.CONDITIONING_BODYWEIGHT].orEmpty()

        return when (stationCount) {
            3 -> listOf(
                cardio.random().id,
                (lower + upper).random().id,
                (core + conditioning).random().id
            )
            4 -> listOf(
                cardio.random().id,
                lower.random().id,
                upper.random().id,
                (core + conditioning).random().id
            )
            else -> error("Unsupported station count: $stationCount")
        }
    }

    private fun fingerprint(ids: List<String>): String = ids.sorted().joinToString(",")
}

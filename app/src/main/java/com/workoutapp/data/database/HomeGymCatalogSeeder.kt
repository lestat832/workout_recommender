package com.workoutapp.data.database

import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.ExerciseCategory
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType

/**
 * Converts the [HomeGymMovementCatalog] into [Exercise] domain objects ready
 * for insertion into the database. Each movement's bucket maps to:
 *
 *  - `exerciseCategory` — so LMU's category-based generator filters correctly
 *    (cardio and conditioning/bodyweight stay out of LMU strength workouts)
 *  - legacy `category: WorkoutType` — kept for any code path still on the old
 *    taxonomy; best-effort based on movement pattern
 *  - `muscleGroups` — legitimate muscle group tags for LMU's muscle-group
 *    picker if the exercise ever surfaces there
 *
 * Upper body is split into push/pull by movement-pattern keyword matching on
 * the movement name.
 */
object HomeGymCatalogSeeder {

    fun buildExercises(): List<Exercise> =
        HomeGymMovementCatalog.movements.map { it.toExercise() }

    private fun HomeGymMovementCatalog.Movement.toExercise(): Exercise {
        val (muscleGroups, exerciseCategory, legacyCategory) = categorize(this)
        return Exercise(
            id = id,
            name = name,
            muscleGroups = muscleGroups,
            equipment = equipment,
            category = legacyCategory,
            exerciseCategory = exerciseCategory,
            imageUrl = null,
            instructions = listOf(instructions),
            isUserCreated = false
        )
    }

    private data class Categorization(
        val muscleGroups: List<MuscleGroup>,
        val exerciseCategory: ExerciseCategory,
        val legacyCategory: WorkoutType
    )

    private fun categorize(movement: HomeGymMovementCatalog.Movement): Categorization =
        when (movement.bucket) {
            HomeGymMovementCatalog.Bucket.CARDIO -> Categorization(
                muscleGroups = emptyList(),
                exerciseCategory = ExerciseCategory.CARDIO_CONDITIONING,
                legacyCategory = WorkoutType.PUSH
            )

            HomeGymMovementCatalog.Bucket.LOWER_BODY -> Categorization(
                muscleGroups = listOf(MuscleGroup.LEGS),
                exerciseCategory = ExerciseCategory.STRENGTH_LEGS,
                legacyCategory = WorkoutType.PULL
            )

            HomeGymMovementCatalog.Bucket.UPPER_BODY -> {
                val lower = movement.name.lowercase()
                val isPull = "row" in lower
                if (isPull) {
                    Categorization(
                        muscleGroups = listOf(MuscleGroup.BACK, MuscleGroup.BICEP),
                        exerciseCategory = ExerciseCategory.STRENGTH_PULL,
                        legacyCategory = WorkoutType.PULL
                    )
                } else {
                    Categorization(
                        muscleGroups = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDER, MuscleGroup.TRICEP),
                        exerciseCategory = ExerciseCategory.STRENGTH_PUSH,
                        legacyCategory = WorkoutType.PUSH
                    )
                }
            }

            HomeGymMovementCatalog.Bucket.CORE -> Categorization(
                muscleGroups = listOf(MuscleGroup.CORE),
                exerciseCategory = ExerciseCategory.CORE,
                legacyCategory = WorkoutType.PULL
            )

            HomeGymMovementCatalog.Bucket.CONDITIONING_BODYWEIGHT -> Categorization(
                muscleGroups = emptyList(),
                exerciseCategory = ExerciseCategory.CARDIO_CONDITIONING,
                legacyCategory = WorkoutType.PUSH
            )
        }
}

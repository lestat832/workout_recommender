package com.workoutapp.domain.usecase

import com.workoutapp.domain.model.EquipmentType
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.GymRepository
import com.workoutapp.domain.repository.WorkoutRepository
import javax.inject.Inject
import kotlin.random.Random

class GenerateWorkoutUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val gymRepository: GymRepository
) {
    suspend operator fun invoke(): List<Exercise> {
        // Get default gym for equipment filtering
        val defaultGym = gymRepository.getDefaultGym()

        // Determine workout type based on last workout
        val lastWorkout = workoutRepository.getLastWorkout()
        val workoutType = if (lastWorkout?.type == WorkoutType.PUSH) {
            WorkoutType.PULL
        } else {
            WorkoutType.PUSH
        }

        // Get exercises done in the last week
        val recentExerciseIds = workoutRepository.getExerciseIdsFromLastWeek()

        // Get user's active exercises for this workout type
        // Filter priority: Equipment → Type → Cooldown → Muscle Groups
        val availableExercises = exerciseRepository.getUserActiveExercisesByType(workoutType)
            .filter { exercise ->
                // If we have a default gym, filter by equipment
                defaultGym?.let { gym ->
                    EquipmentType.canPerformExercise(exercise.equipment, gym.equipmentList)
                } ?: true // If no gym set, include all exercises
            }
            .filterNot { it.id in recentExerciseIds }

        // Group by primary muscle group
        val exercisesByMuscle = availableExercises.groupBy { it.muscleGroups.firstOrNull() ?: MuscleGroup.CHEST }

        // Select one exercise per muscle group
        val targetMuscleGroups = if (workoutType == WorkoutType.PUSH) {
            listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDER, MuscleGroup.TRICEP)
        } else {
            listOf(MuscleGroup.LEGS, MuscleGroup.BACK, MuscleGroup.BICEP)
        }

        return targetMuscleGroups.mapNotNull { muscleGroup ->
            exercisesByMuscle[muscleGroup]?.randomOrNull()
        }.take(3)
    }
}
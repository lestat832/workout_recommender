package com.workoutapp.shared.domain.usecase

import com.workoutapp.shared.domain.model.Exercise
import com.workoutapp.shared.domain.model.MuscleGroup
import com.workoutapp.shared.domain.model.WorkoutType
import com.workoutapp.shared.domain.repository.ExerciseRepository
import com.workoutapp.shared.domain.repository.WorkoutRepository
import kotlin.random.Random

class GenerateWorkoutUseCase(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(): List<Exercise> {
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
        val availableExercises = exerciseRepository.getUserActiveExercisesByType(workoutType)
            .filterNot { it.id in recentExerciseIds }
        
        // Group by muscle group
        val exercisesByMuscle = availableExercises.groupBy { it.muscleGroup }
        
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
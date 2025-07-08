package com.workoutapp.domain.usecase

import com.workoutapp.data.database.ExerciseDataV2
import com.workoutapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class InitializeDatabaseUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke() {
        val exercises = exerciseRepository.getAllExercises().first()
        if (exercises.isEmpty()) {
            // Convert ExerciseData entities to domain models and insert
            val domainExercises = ExerciseDataV2.exercises.map { entity ->
                com.workoutapp.domain.model.Exercise(
                    id = entity.id,
                    name = entity.name,
                    muscleGroup = entity.muscleGroup,
                    equipment = entity.equipment,
                    category = entity.category,
                    imageUrl = entity.imageUrl,
                    instructions = entity.instructions,
                    difficulty = entity.difficulty
                )
            }
            
            // Insert exercises using repository
            exerciseRepository.insertExercises(domainExercises)
        }
    }
}
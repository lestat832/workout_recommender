package com.workoutapp.data.repository

import com.workoutapp.data.database.dao.ExerciseDao
import com.workoutapp.data.database.entities.ExerciseEntity
import com.workoutapp.data.database.entities.UserExerciseEntity
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.WorkoutType
import com.workoutapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {
    
    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises().map { entities ->
            entities.map { entity ->
                Exercise(
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
        }
    }
    
    override suspend fun getExercisesByType(workoutType: WorkoutType): List<Exercise> {
        return exerciseDao.getExercisesByType(workoutType).map { entity ->
            Exercise(
                id = entity.id,
                name = entity.name,
                muscleGroup = entity.muscleGroup,
                equipment = entity.equipment,
                category = entity.category
            )
        }
    }
    
    override suspend fun getUserActiveExercisesByType(workoutType: WorkoutType): List<Exercise> {
        return exerciseDao.getUserActiveExercisesByType(workoutType).map { entity ->
            Exercise(
                id = entity.id,
                name = entity.name,
                muscleGroup = entity.muscleGroup,
                equipment = entity.equipment,
                category = entity.category
            )
        }
    }
    
    override suspend fun setUserExercises(exerciseIds: List<String>) {
        exerciseIds.forEach { exerciseId ->
            exerciseDao.insertUserExercise(
                UserExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    exerciseId = exerciseId,
                    isActive = true
                )
            )
        }
    }
    
    override suspend fun updateUserExerciseStatus(exerciseId: String, isActive: Boolean) {
        exerciseDao.updateUserExerciseStatus(exerciseId, isActive)
    }
    
    override fun getActiveUserExercises(): Flow<List<String>> {
        return exerciseDao.getActiveUserExercises().map { entities ->
            entities.map { it.exerciseId }
        }
    }
    
    override suspend fun insertExercises(exercises: List<Exercise>) {
        val entities = exercises.map { exercise ->
            ExerciseEntity(
                id = exercise.id,
                name = exercise.name,
                muscleGroup = exercise.muscleGroup,
                equipment = exercise.equipment,
                category = exercise.category,
                imageUrl = exercise.imageUrl,
                instructions = exercise.instructions,
                difficulty = exercise.difficulty
            )
        }
        exerciseDao.insertExercises(entities)
    }
}
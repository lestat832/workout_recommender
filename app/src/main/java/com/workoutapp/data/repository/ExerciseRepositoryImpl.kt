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
                    muscleGroups = entity.muscleGroups,
                    equipment = entity.equipment,
                    category = entity.category,
                    imageUrl = entity.imageUrl,
                    instructions = entity.instructions,
                    isUserCreated = entity.isUserCreated
                )
            }
        }
    }
    
    override suspend fun getExercisesByType(workoutType: WorkoutType): List<Exercise> {
        return exerciseDao.getExercisesByType(workoutType).map { entity ->
            Exercise(
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
    }
    
    override suspend fun getUserActiveExercisesByType(workoutType: WorkoutType): List<Exercise> {
        return exerciseDao.getUserActiveExercisesByType(workoutType).map { entity ->
            Exercise(
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
                muscleGroups = exercise.muscleGroups,
                equipment = exercise.equipment,
                category = exercise.category,
                imageUrl = exercise.imageUrl,
                instructions = exercise.instructions,
                isUserCreated = exercise.isUserCreated,
                createdAt = if (exercise.isUserCreated) System.currentTimeMillis() else null
            )
        }
        exerciseDao.insertExercises(entities)
    }
    
    override suspend fun createCustomExercise(exercise: Exercise) {
        val entity = ExerciseEntity(
            id = exercise.id,
            name = exercise.name,
            muscleGroups = exercise.muscleGroups,
            equipment = exercise.equipment,
            category = exercise.category,
            imageUrl = exercise.imageUrl,
            instructions = exercise.instructions,
            isUserCreated = true,
            createdAt = System.currentTimeMillis()
        )
        exerciseDao.insertExercise(entity)
    }
    
    override suspend fun getCustomExerciseByName(name: String): Exercise? {
        return exerciseDao.getCustomExerciseByName(name)?.let { entity ->
            Exercise(
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
    }
    
    override fun getCustomExercises(): Flow<List<Exercise>> {
        return exerciseDao.getCustomExercises().map { entities ->
            entities.map { entity ->
                Exercise(
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
        }
    }
}
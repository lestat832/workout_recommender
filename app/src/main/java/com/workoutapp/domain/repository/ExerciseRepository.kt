package com.workoutapp.domain.repository

import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.ExerciseCategory
import com.workoutapp.domain.model.WorkoutType
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    suspend fun getExerciseById(id: String): Exercise?
    suspend fun getExercisesByType(workoutType: WorkoutType): List<Exercise>
    suspend fun getUserActiveExercisesByType(workoutType: WorkoutType): List<Exercise>
    suspend fun getUserActiveExercisesByCategories(categories: List<ExerciseCategory>): List<Exercise>
    suspend fun setUserExercises(exerciseIds: List<String>)
    suspend fun updateUserExerciseStatus(exerciseId: String, isActive: Boolean)
    fun getActiveUserExercises(): Flow<List<String>>
    suspend fun insertExercises(exercises: List<Exercise>)
    suspend fun createCustomExercise(exercise: Exercise)
    suspend fun getCustomExerciseByName(name: String): Exercise?
    fun getCustomExercises(): Flow<List<Exercise>>
    suspend fun backfillExerciseCategories()
    suspend fun reclassifyCardioByName()
}
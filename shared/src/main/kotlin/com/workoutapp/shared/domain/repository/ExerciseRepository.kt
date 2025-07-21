package com.workoutapp.shared.domain.repository

import com.workoutapp.shared.domain.model.Exercise
import com.workoutapp.shared.domain.model.WorkoutType
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    suspend fun getExercisesByType(workoutType: WorkoutType): List<Exercise>
    suspend fun getUserActiveExercisesByType(workoutType: WorkoutType): List<Exercise>
    suspend fun setUserExercises(exerciseIds: List<String>)
    suspend fun updateUserExerciseStatus(exerciseId: String, isActive: Boolean)
    fun getActiveUserExercises(): Flow<List<String>>
    suspend fun insertExercises(exercises: List<Exercise>)
}
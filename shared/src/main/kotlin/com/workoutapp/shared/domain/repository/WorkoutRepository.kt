package com.workoutapp.shared.domain.repository

import com.workoutapp.shared.domain.model.Workout
import com.workoutapp.shared.domain.model.WorkoutExercise
import com.workoutapp.shared.domain.model.WorkoutStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface WorkoutRepository {
    suspend fun createWorkout(workout: Workout): String
    suspend fun updateWorkout(workout: Workout)
    suspend fun deleteWorkout(workoutId: String)
    suspend fun getWorkoutById(id: String): Workout?
    suspend fun getLastWorkout(): Workout?
    fun getWorkoutsByStatus(status: WorkoutStatus): Flow<List<Workout>>
    suspend fun addExerciseToWorkout(workoutId: String, exercise: WorkoutExercise)
    suspend fun updateWorkoutExercise(exercise: WorkoutExercise)
    suspend fun getExerciseIdsFromLastWeek(): List<String>
    fun getAllWorkouts(): Flow<List<Workout>>
}
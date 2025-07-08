package com.workoutapp.domain.repository

import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutExercise
import com.workoutapp.domain.model.WorkoutStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface WorkoutRepository {
    suspend fun createWorkout(workout: Workout): String
    suspend fun updateWorkout(workout: Workout)
    suspend fun getWorkoutById(id: String): Workout?
    suspend fun getLastWorkout(): Workout?
    fun getWorkoutsByStatus(status: WorkoutStatus): Flow<List<Workout>>
    suspend fun addExerciseToWorkout(workoutId: String, exercise: WorkoutExercise)
    suspend fun updateWorkoutExercise(exercise: WorkoutExercise)
    suspend fun getExerciseIdsFromLastWeek(): List<String>
    fun getAllWorkouts(): Flow<List<Workout>>
}
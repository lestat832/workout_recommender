package com.workoutapp.domain.repository

import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutExercise
import com.workoutapp.domain.model.WorkoutStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface WorkoutRepository {
    suspend fun createWorkout(workout: Workout): String
    suspend fun updateWorkout(workout: Workout)
    suspend fun deleteWorkout(workoutId: String)
    suspend fun getWorkoutById(id: String): Workout?
    suspend fun getLastWorkout(): Workout?
    suspend fun getLastCompletedWorkoutByGym(gymId: Long): Workout?
    suspend fun getConditioningWorkoutsInMonth(gymId: Long): List<Workout>
    fun getWorkoutsByStatus(status: WorkoutStatus): Flow<List<Workout>>
    suspend fun addExerciseToWorkout(workoutId: String, exercise: WorkoutExercise)
    suspend fun updateWorkoutExercise(exercise: WorkoutExercise)
    suspend fun getExerciseIdsFromLastWeek(): List<String>
    fun getAllWorkouts(): Flow<List<Workout>>
    suspend fun getAllCompletedWorkoutsWithExercises(): List<Workout>
    suspend fun reassignWorkouts(oldGymId: Long, newGymId: Long)
    fun getCompletedWorkoutsByGym(gymId: Long): Flow<List<Workout>>
    suspend fun getInProgressStrengthWorkout(gymId: Long): Workout?
    suspend fun getInProgressConditioningWorkout(gymId: Long): Workout?
}
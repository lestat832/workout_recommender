package com.workoutapp.data.database.dao

import androidx.room.*
import com.workoutapp.data.database.entities.WorkoutEntity
import com.workoutapp.data.database.entities.WorkoutExerciseEntity
import com.workoutapp.domain.model.WorkoutStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercises(exercises: List<WorkoutExerciseEntity>)
    
    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)
    
    @Update
    suspend fun updateWorkoutExercise(exercise: WorkoutExerciseEntity)
    
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: String): WorkoutEntity?
    
    @Query("SELECT * FROM workouts ORDER BY date DESC LIMIT 1")
    suspend fun getLastWorkout(): WorkoutEntity?
    
    @Query("SELECT * FROM workouts WHERE status = :status ORDER BY date DESC")
    fun getWorkoutsByStatus(status: WorkoutStatus): Flow<List<WorkoutEntity>>
    
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun getWorkoutExercises(workoutId: String): List<WorkoutExerciseEntity>
    
    @Query("""
        SELECT DISTINCT we.exerciseId 
        FROM workout_exercises we 
        INNER JOIN workouts w ON we.workoutId = w.id 
        WHERE w.date >= :startDate
    """)
    suspend fun getExerciseIdsFromDate(startDate: Date): List<String>
    
    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>
}
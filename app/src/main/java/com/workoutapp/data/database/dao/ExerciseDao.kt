package com.workoutapp.data.database.dao

import androidx.room.*
import com.workoutapp.data.database.entities.ExerciseEntity
import com.workoutapp.data.database.entities.UserExerciseEntity
import com.workoutapp.domain.model.WorkoutType
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<ExerciseEntity>>
    
    @Query("SELECT * FROM exercises WHERE category = :workoutType")
    suspend fun getExercisesByType(workoutType: WorkoutType): List<ExerciseEntity>
    
    @Query("SELECT e.* FROM exercises e INNER JOIN user_exercises ue ON e.id = ue.exerciseId WHERE ue.isActive = 1 AND e.category = :workoutType")
    suspend fun getUserActiveExercisesByType(workoutType: WorkoutType): List<ExerciseEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserExercise(userExercise: UserExerciseEntity)
    
    @Query("UPDATE user_exercises SET isActive = :isActive WHERE exerciseId = :exerciseId")
    suspend fun updateUserExerciseStatus(exerciseId: String, isActive: Boolean)
    
    @Query("SELECT * FROM user_exercises WHERE isActive = 1")
    fun getActiveUserExercises(): Flow<List<UserExerciseEntity>>
    
    @Query("SELECT * FROM exercises WHERE isUserCreated = 1 ORDER BY createdAt DESC")
    fun getCustomExercises(): Flow<List<ExerciseEntity>>
    
    @Query("DELETE FROM exercises WHERE id = :exerciseId AND isUserCreated = 1")
    suspend fun deleteCustomExercise(exerciseId: String)
    
    @Query("SELECT * FROM exercises WHERE LOWER(name) = LOWER(:name) AND isUserCreated = 1 LIMIT 1")
    suspend fun getCustomExerciseByName(name: String): ExerciseEntity?
}
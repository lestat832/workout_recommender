package com.workoutapp.data.database.dao

import androidx.room.*
import com.workoutapp.data.database.entities.ExerciseEntity
import com.workoutapp.data.database.entities.UserExerciseEntity
import com.workoutapp.domain.model.ExerciseCategory
import com.workoutapp.domain.model.WorkoutType
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getExerciseById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE category = :workoutType")
    suspend fun getExercisesByType(workoutType: WorkoutType): List<ExerciseEntity>

    @Query("SELECT e.* FROM exercises e INNER JOIN user_exercises ue ON e.id = ue.exerciseId WHERE ue.isActive = 1 AND e.category = :workoutType")
    suspend fun getUserActiveExercisesByType(workoutType: WorkoutType): List<ExerciseEntity>

    @Query("""
        SELECT DISTINCT e.* FROM exercises e
        INNER JOIN user_exercises ue ON e.id = ue.exerciseId
        WHERE ue.isActive = 1 AND e.exerciseCategory IN (:categories)
    """)
    suspend fun getUserActiveExercisesByCategories(
        categories: List<ExerciseCategory>
    ): List<ExerciseEntity>

    @Query("UPDATE exercises SET exerciseCategory = 'STRENGTH_PULL' WHERE category = 'PULL'")
    suspend fun backfillPullCategory()

    @Query("UPDATE exercises SET exerciseCategory = 'STRENGTH_PUSH' WHERE category = 'PUSH'")
    suspend fun backfillPushCategory()

    @Query("UPDATE exercises SET exerciseCategory = 'STRENGTH_LEGS' WHERE muscleGroups LIKE '%LEGS%'")
    suspend fun backfillLegsCategory()

    @Query("UPDATE exercises SET exerciseCategory = 'CORE' WHERE muscleGroups LIKE '%CORE%' AND exerciseCategory != 'STRENGTH_LEGS'")
    suspend fun backfillCoreCategory()

    /**
     * Reclassifies cardio/plyometric exercises that were previously bucketed
     * as strength due to having LEGS or CORE in their muscle groups. Matches
     * on name fragments so exercises like "Fast Skipping", "Jumping Jack",
     * "Burpee", etc. get tagged as CARDIO_CONDITIONING regardless of their
     * original muscle group assignment.
     */
    @Query("""
        UPDATE exercises
        SET exerciseCategory = 'CARDIO_CONDITIONING'
        WHERE LOWER(name) LIKE '%skipping%'
           OR LOWER(name) LIKE '%jump rope%'
           OR LOWER(name) LIKE '%jumping jack%'
           OR LOWER(name) LIKE '%mountain climber%'
           OR LOWER(name) LIKE '%high knee%'
           OR LOWER(name) LIKE '%burpee%'
           OR LOWER(name) LIKE '%sprint%'
           OR LOWER(name) LIKE '%running%'
           OR LOWER(name) LIKE '%skater%'
    """)
    suspend fun reclassifyCardioByName()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserExercise(userExercise: UserExerciseEntity)

    @Query("SELECT COUNT(*) > 0 FROM user_exercises WHERE exerciseId = :exerciseId")
    suspend fun isUserExerciseExists(exerciseId: String): Boolean

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

    @Query("SELECT * FROM exercises WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getExerciseByName(name: String): ExerciseEntity?
}
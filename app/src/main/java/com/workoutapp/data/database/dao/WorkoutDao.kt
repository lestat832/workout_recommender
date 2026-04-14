package com.workoutapp.data.database.dao

import androidx.room.*
import com.workoutapp.data.database.entities.WorkoutEntity
import com.workoutapp.data.database.entities.WorkoutExerciseEntity
import com.workoutapp.domain.model.WorkoutStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

data class ExerciseLastPerformed(
    val exerciseId: String,
    val lastDate: Date
)

data class CompletedWorkoutSummaryRow(
    val id: String,
    val date: Date,
    val format: String,
    val durationMinutes: Int?,
    // Nested separators: '|' between workouts' exercises, ',' within a single exercise's
    // muscleGroups (Converters.kt convention). Repository splits on both.
    val muscleGroupsRaw: String
)

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
    
    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: String)
    
    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteWorkoutExercises(workoutId: String)
    
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: String): WorkoutEntity?
    
    @Query("SELECT * FROM workouts ORDER BY date DESC LIMIT 1")
    suspend fun getLastWorkout(): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE gymId = :gymId AND status = 'COMPLETED' ORDER BY date DESC LIMIT 1")
    suspend fun getLastCompletedWorkoutByGym(gymId: Long): WorkoutEntity?

    @Query("""
        SELECT * FROM workouts
        WHERE gymId = :gymId
          AND format IN ('EMOM','AMRAP')
          AND date >= :startDate
          AND date < :endDate
        ORDER BY date DESC
    """)
    suspend fun getConditioningWorkoutsInRange(
        gymId: Long,
        startDate: Date,
        endDate: Date
    ): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE status = :status ORDER BY date DESC")
    fun getWorkoutsByStatus(status: WorkoutStatus): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun getWorkoutExercises(workoutId: String): List<WorkoutExerciseEntity>

    @Query("""
        SELECT DISTINCT we.exerciseId
        FROM workout_exercises we
        INNER JOIN workouts w ON we.workoutId = w.id
        WHERE w.date >= :startDate AND w.status = 'COMPLETED'
    """)
    suspend fun getExerciseIdsFromDate(startDate: Date): List<String>
    
    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' ORDER BY date DESC")
    suspend fun getAllCompletedWorkouts(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE gymId = :gymId AND status = 'COMPLETED' ORDER BY date DESC")
    fun getCompletedWorkoutsByGym(gymId: Long): Flow<List<WorkoutEntity>>

    @Query("UPDATE workouts SET gymId = :newGymId WHERE gymId = :oldGymId")
    suspend fun reassignWorkouts(oldGymId: Long, newGymId: Long)

    @Query("SELECT * FROM workouts WHERE gymId = :gymId AND status IN ('IN_PROGRESS', 'INCOMPLETE') AND format = 'STRENGTH' ORDER BY date DESC LIMIT 1")
    suspend fun getInProgressStrengthWorkout(gymId: Long): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE gymId = :gymId AND status = 'IN_PROGRESS' AND format IN ('EMOM', 'AMRAP') ORDER BY date DESC LIMIT 1")
    suspend fun getInProgressConditioningWorkout(gymId: Long): WorkoutEntity?

    @Query("""
        SELECT we.exerciseId, MAX(w.date) as lastDate
        FROM workout_exercises we
        INNER JOIN workouts w ON we.workoutId = w.id
        WHERE w.status = 'COMPLETED'
        GROUP BY we.exerciseId
    """)
    suspend fun getExerciseLastPerformedDates(): List<ExerciseLastPerformed>

    /**
     * Returns completed workouts on or after `sinceMillis` with exercise muscle groups
     * concatenated with '|'. Used by FatigueAwareness via the repository.
     */
    @Query("""
        SELECT
            w.id AS id,
            w.date AS date,
            w.format AS format,
            w.durationMinutes AS durationMinutes,
            GROUP_CONCAT(e.muscleGroups, '|') AS muscleGroupsRaw
        FROM workouts w
        INNER JOIN workout_exercises we ON we.workoutId = w.id
        INNER JOIN exercises e ON e.id = we.exerciseId
        WHERE w.status = 'COMPLETED' AND w.date >= :sinceMillis
        GROUP BY w.id
        ORDER BY w.date DESC
    """)
    suspend fun getCompletedWorkoutSummariesSince(sinceMillis: Long): List<CompletedWorkoutSummaryRow>
}
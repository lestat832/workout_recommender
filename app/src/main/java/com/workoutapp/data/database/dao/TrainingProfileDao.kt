package com.workoutapp.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.workoutapp.data.database.entities.ExerciseProfileEntity
import com.workoutapp.data.database.entities.GlobalProfileEntity
import com.workoutapp.data.database.entities.MuscleGroupProfileEntity

@Dao
interface TrainingProfileDao {

    // ── Exercise profiles ───────────────────────────────────────────────

    @Query("SELECT * FROM exercise_profiles WHERE exerciseId = :exerciseId")
    suspend fun getExerciseProfile(exerciseId: String): ExerciseProfileEntity?

    @Query("SELECT * FROM exercise_profiles WHERE exerciseId IN (:exerciseIds)")
    suspend fun getExerciseProfiles(exerciseIds: List<String>): List<ExerciseProfileEntity>

    @Query("SELECT * FROM exercise_profiles ORDER BY lastPerformedDate DESC")
    suspend fun getAllExerciseProfiles(): List<ExerciseProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExerciseProfile(profile: ExerciseProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExerciseProfiles(profiles: List<ExerciseProfileEntity>)

    // ── Muscle group profiles ───────────────────────────────────────────

    @Query("SELECT * FROM muscle_group_profiles WHERE muscleGroup = :muscleGroup")
    suspend fun getMuscleGroupProfile(muscleGroup: String): MuscleGroupProfileEntity?

    @Query("SELECT * FROM muscle_group_profiles")
    suspend fun getAllMuscleGroupProfiles(): List<MuscleGroupProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMuscleGroupProfile(profile: MuscleGroupProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMuscleGroupProfiles(profiles: List<MuscleGroupProfileEntity>)

    // ── Global profile ──────────────────────────────────────────────────

    @Query("SELECT * FROM global_profile WHERE id = 1")
    suspend fun getGlobalProfile(): GlobalProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGlobalProfile(profile: GlobalProfileEntity)

    // ── Bulk operations ─────────────────────────────────────────────────

    @Query("DELETE FROM exercise_profiles")
    suspend fun deleteAllExerciseProfiles()

    @Query("DELETE FROM muscle_group_profiles")
    suspend fun deleteAllMuscleGroupProfiles()

    @Query("DELETE FROM global_profile")
    suspend fun deleteAllGlobalProfiles()

    @Transaction
    suspend fun deleteAndRebuildProfiles(
        exerciseProfiles: List<ExerciseProfileEntity>,
        muscleGroupProfiles: List<MuscleGroupProfileEntity>,
        globalProfile: GlobalProfileEntity
    ) {
        deleteAllExerciseProfiles()
        deleteAllMuscleGroupProfiles()
        deleteAllGlobalProfiles()
        upsertExerciseProfiles(exerciseProfiles)
        upsertMuscleGroupProfiles(muscleGroupProfiles)
        upsertGlobalProfile(globalProfile)
    }
}

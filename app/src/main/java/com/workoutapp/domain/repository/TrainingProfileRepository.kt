package com.workoutapp.domain.repository

import com.workoutapp.domain.model.ExerciseProfile
import com.workoutapp.domain.model.GlobalProfile
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.MuscleGroupProfile

interface TrainingProfileRepository {

    suspend fun getExerciseProfile(exerciseId: String): ExerciseProfile?

    suspend fun getExerciseProfiles(exerciseIds: List<String>): List<ExerciseProfile>

    suspend fun getAllExerciseProfiles(): List<ExerciseProfile>

    suspend fun saveExerciseProfile(profile: ExerciseProfile)

    suspend fun saveExerciseProfiles(profiles: List<ExerciseProfile>)

    suspend fun getMuscleGroupProfile(muscleGroup: MuscleGroup): MuscleGroupProfile?

    suspend fun getAllMuscleGroupProfiles(): List<MuscleGroupProfile>

    suspend fun saveMuscleGroupProfile(profile: MuscleGroupProfile)

    suspend fun saveMuscleGroupProfiles(profiles: List<MuscleGroupProfile>)

    suspend fun getGlobalProfile(): GlobalProfile?

    suspend fun saveGlobalProfile(profile: GlobalProfile)

    suspend fun deleteAllProfiles()

    suspend fun deleteAndRebuildProfiles(
        exerciseProfiles: List<ExerciseProfile>,
        muscleGroupProfiles: List<MuscleGroupProfile>,
        globalProfile: GlobalProfile
    )
}

package com.workoutapp.data.repository

import com.workoutapp.data.database.dao.TrainingProfileDao
import com.workoutapp.data.database.entities.ExerciseProfileEntity
import com.workoutapp.data.database.entities.GlobalProfileEntity
import com.workoutapp.data.database.entities.MuscleGroupProfileEntity
import com.workoutapp.domain.model.ExerciseProfile
import com.workoutapp.domain.model.GlobalProfile
import com.workoutapp.domain.model.LoadingPattern
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.MuscleGroupProfile
import com.workoutapp.domain.repository.TrainingProfileRepository
import javax.inject.Inject

class TrainingProfileRepositoryImpl @Inject constructor(
    private val dao: TrainingProfileDao
) : TrainingProfileRepository {

    override suspend fun getExerciseProfile(exerciseId: String): ExerciseProfile? {
        return dao.getExerciseProfile(exerciseId)?.toDomain()
    }

    override suspend fun getExerciseProfiles(exerciseIds: List<String>): List<ExerciseProfile> {
        return dao.getExerciseProfiles(exerciseIds).map { it.toDomain() }
    }

    override suspend fun getAllExerciseProfiles(): List<ExerciseProfile> {
        return dao.getAllExerciseProfiles().map { it.toDomain() }
    }

    override suspend fun saveExerciseProfile(profile: ExerciseProfile) {
        dao.upsertExerciseProfile(profile.toEntity())
    }

    override suspend fun saveExerciseProfiles(profiles: List<ExerciseProfile>) {
        dao.upsertExerciseProfiles(profiles.map { it.toEntity() })
    }

    override suspend fun getMuscleGroupProfile(muscleGroup: MuscleGroup): MuscleGroupProfile? {
        return dao.getMuscleGroupProfile(muscleGroup.name)?.toDomain()
    }

    override suspend fun getAllMuscleGroupProfiles(): List<MuscleGroupProfile> {
        return dao.getAllMuscleGroupProfiles().map { it.toDomain() }
    }

    override suspend fun saveMuscleGroupProfile(profile: MuscleGroupProfile) {
        dao.upsertMuscleGroupProfile(profile.toEntity())
    }

    override suspend fun saveMuscleGroupProfiles(profiles: List<MuscleGroupProfile>) {
        dao.upsertMuscleGroupProfiles(profiles.map { it.toEntity() })
    }

    override suspend fun getGlobalProfile(): GlobalProfile? {
        return dao.getGlobalProfile()?.toDomain()
    }

    override suspend fun saveGlobalProfile(profile: GlobalProfile) {
        dao.upsertGlobalProfile(profile.toEntity())
    }

    override suspend fun deleteAllProfiles() {
        dao.deleteAllExerciseProfiles()
        dao.deleteAllMuscleGroupProfiles()
        dao.deleteAllGlobalProfiles()
    }

    override suspend fun deleteAndRebuildProfiles(
        exerciseProfiles: List<ExerciseProfile>,
        muscleGroupProfiles: List<MuscleGroupProfile>,
        globalProfile: GlobalProfile
    ) {
        dao.deleteAndRebuildProfiles(
            exerciseProfiles = exerciseProfiles.map { it.toEntity() },
            muscleGroupProfiles = muscleGroupProfiles.map { it.toEntity() },
            globalProfile = globalProfile.toEntity()
        )
    }

    // ── Entity ↔ Domain mapping ─────────────────────────────────────────

    private fun ExerciseProfileEntity.toDomain() = ExerciseProfile(
        exerciseId = exerciseId,
        loadingPattern = try { LoadingPattern.valueOf(loadingPattern) } catch (_: Exception) { LoadingPattern.UNKNOWN },
        loadingPatternConfidence = loadingPatternConfidence,
        currentWorkingWeight = currentWorkingWeight,
        warmupWeight = warmupWeight,
        rampSteps = rampSteps,
        bodyweightOnly = bodyweightOnly,
        preferredWorkingSets = preferredWorkingSets,
        preferredRepsMin = preferredRepsMin,
        preferredRepsMax = preferredRepsMax,
        lastProgressionDate = lastProgressionDate,
        progressionRateLbPerMonth = progressionRateLbPerMonth,
        estimatedOneRepMax = estimatedOneRepMax,
        plateauFlag = plateauFlag,
        plateauSessionCount = plateauSessionCount,
        sessionCount = sessionCount,
        strengthSessionCount = strengthSessionCount,
        lastPerformedDate = lastPerformedDate
    )

    private fun ExerciseProfile.toEntity() = ExerciseProfileEntity(
        exerciseId = exerciseId,
        loadingPattern = loadingPattern.name,
        loadingPatternConfidence = loadingPatternConfidence,
        currentWorkingWeight = currentWorkingWeight,
        warmupWeight = warmupWeight,
        rampSteps = rampSteps,
        bodyweightOnly = bodyweightOnly,
        preferredWorkingSets = preferredWorkingSets,
        preferredRepsMin = preferredRepsMin,
        preferredRepsMax = preferredRepsMax,
        lastProgressionDate = lastProgressionDate,
        progressionRateLbPerMonth = progressionRateLbPerMonth,
        estimatedOneRepMax = estimatedOneRepMax,
        plateauFlag = plateauFlag,
        plateauSessionCount = plateauSessionCount,
        sessionCount = sessionCount,
        strengthSessionCount = strengthSessionCount,
        lastPerformedDate = lastPerformedDate,
        lastUpdated = System.currentTimeMillis()
    )

    private fun MuscleGroupProfileEntity.toDomain() = MuscleGroupProfile(
        muscleGroup = try { MuscleGroup.valueOf(muscleGroup) } catch (_: Exception) { MuscleGroup.CHEST },
        weeklySetVolume = weeklySetVolume,
        volumeTolerance = volumeTolerance,
        coveragePercentage = coveragePercentage,
        preferredExerciseIds = preferredExerciseIds.split(",").filter { it.isNotEmpty() },
        avoidedExerciseIds = avoidedExerciseIds.split(",").filter { it.isNotEmpty() },
        lastTrainedDate = lastTrainedDate,
        sessionCount = sessionCount
    )

    private fun MuscleGroupProfile.toEntity() = MuscleGroupProfileEntity(
        muscleGroup = muscleGroup.name,
        weeklySetVolume = weeklySetVolume,
        volumeTolerance = volumeTolerance,
        coveragePercentage = coveragePercentage,
        preferredExerciseIds = preferredExerciseIds.joinToString(","),
        avoidedExerciseIds = avoidedExerciseIds.joinToString(","),
        lastTrainedDate = lastTrainedDate,
        sessionCount = sessionCount,
        lastUpdated = System.currentTimeMillis()
    )

    private fun GlobalProfileEntity.toDomain() = GlobalProfile(
        avgSessionDurationMin = avgSessionDurationMin,
        avgExercisesPerSession = avgExercisesPerSession,
        avgSetsPerExercise = avgSetsPerExercise,
        trainingFrequencyPerWeek = trainingFrequencyPerWeek,
        pushPullRatio = pushPullRatio,
        preferredTrainingDays = preferredTrainingDays.split(",").filter { it.isNotEmpty() },
        totalCompletedSessions = totalCompletedSessions,
        totalStrengthSessions = totalStrengthSessions,
        totalConditioningSessions = totalConditioningSessions,
        currentStreakWeeks = currentStreakWeeks,
        lastWorkoutDate = lastWorkoutDate,
        lastFullRecompute = lastFullRecompute
    )

    private fun GlobalProfile.toEntity() = GlobalProfileEntity(
        id = 1,
        avgSessionDurationMin = avgSessionDurationMin,
        avgExercisesPerSession = avgExercisesPerSession,
        avgSetsPerExercise = avgSetsPerExercise,
        trainingFrequencyPerWeek = trainingFrequencyPerWeek,
        pushPullRatio = pushPullRatio,
        preferredTrainingDays = preferredTrainingDays.joinToString(","),
        totalCompletedSessions = totalCompletedSessions,
        totalStrengthSessions = totalStrengthSessions,
        totalConditioningSessions = totalConditioningSessions,
        currentStreakWeeks = currentStreakWeeks,
        lastWorkoutDate = lastWorkoutDate,
        lastFullRecompute = lastFullRecompute,
        lastUpdated = System.currentTimeMillis()
    )
}

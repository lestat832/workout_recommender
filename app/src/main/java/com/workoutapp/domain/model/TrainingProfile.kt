package com.workoutapp.domain.model

data class ExerciseProfile(
    val exerciseId: String,
    val loadingPattern: LoadingPattern = LoadingPattern.UNKNOWN,
    val loadingPatternConfidence: Int = 0,
    val currentWorkingWeight: Float? = null,
    val warmupWeight: Float? = null,
    val rampSteps: Int = 0,
    val bodyweightOnly: Boolean = false,
    val preferredWorkingSets: Int = 3,
    val preferredRepsMin: Int = 8,
    val preferredRepsMax: Int = 10,
    val lastProgressionDate: Long? = null,
    val progressionRateLbPerMonth: Float? = null,
    val estimatedOneRepMax: Float? = null,
    val plateauFlag: Boolean = false,
    val plateauSessionCount: Int = 0,
    val sessionCount: Int = 0,
    val strengthSessionCount: Int = 0,
    val lastPerformedDate: Long? = null
)

data class MuscleGroupProfile(
    val muscleGroup: MuscleGroup,
    val weeklySetVolume: Float = 0f,
    val volumeTolerance: Int? = null,
    val coveragePercentage: Float = 0f,
    val preferredExerciseIds: List<String> = emptyList(),
    val avoidedExerciseIds: List<String> = emptyList(),
    val lastTrainedDate: Long? = null,
    val sessionCount: Int = 0
)

data class GlobalProfile(
    val avgSessionDurationMin: Float = 0f,
    val avgExercisesPerSession: Float = 0f,
    val avgSetsPerExercise: Float = 0f,
    val trainingFrequencyPerWeek: Float = 0f,
    val pushPullRatio: Float = 1f,
    val preferredTrainingDays: List<String> = emptyList(),
    val totalCompletedSessions: Int = 0,
    val totalStrengthSessions: Int = 0,
    val totalConditioningSessions: Int = 0,
    val currentStreakWeeks: Int = 0,
    val lastWorkoutDate: Long? = null,
    val lastFullRecompute: Long = 0
)

enum class LoadingPattern {
    RAMP_AND_HOLD,
    FLAT,
    ASCENDING_RAMP,
    UNKNOWN
}

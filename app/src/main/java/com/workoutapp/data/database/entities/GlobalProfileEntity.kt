package com.workoutapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "global_profile")
data class GlobalProfileEntity(
    @PrimaryKey
    val id: Int = 1,

    val avgSessionDurationMin: Float = 0f,
    val avgExercisesPerSession: Float = 0f,
    val avgSetsPerExercise: Float = 0f,
    val trainingFrequencyPerWeek: Float = 0f,
    val pushPullRatio: Float = 1f,
    val preferredTrainingDays: String = "",
    val totalCompletedSessions: Int = 0,
    val totalStrengthSessions: Int = 0,
    val totalConditioningSessions: Int = 0,
    val currentStreakWeeks: Int = 0,
    val lastWorkoutDate: Long? = null,
    val profileVersion: Int = 1,
    val lastFullRecompute: Long = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

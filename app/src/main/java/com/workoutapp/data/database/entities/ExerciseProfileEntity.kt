package com.workoutapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_profiles")
data class ExerciseProfileEntity(
    @PrimaryKey
    val exerciseId: String,

    // Loading pattern (derived from STRENGTH workouts only)
    val loadingPattern: String = "UNKNOWN",
    val loadingPatternConfidence: Int = 0,

    // Weight tracking (derived from STRENGTH workouts only)
    val currentWorkingWeight: Float? = null,
    val warmupWeight: Float? = null,
    val rampSteps: Int = 0,
    val bodyweightOnly: Boolean = false,

    // Set/rep preferences (derived from STRENGTH workouts only)
    val preferredWorkingSets: Int = 3,
    val preferredRepsMin: Int = 8,
    val preferredRepsMax: Int = 10,

    // Progression (derived from STRENGTH workouts only)
    val lastProgressionDate: Long? = null,
    val progressionRateLbPerMonth: Float? = null,
    val estimatedOneRepMax: Float? = null,
    val plateauFlag: Boolean = false,
    val plateauSessionCount: Int = 0,

    // Meta (derived from ALL workout formats)
    val sessionCount: Int = 0,
    val strengthSessionCount: Int = 0,
    val lastPerformedDate: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

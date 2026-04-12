package com.workoutapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "muscle_group_profiles")
data class MuscleGroupProfileEntity(
    @PrimaryKey
    val muscleGroup: String,

    val weeklySetVolume: Float = 0f,
    val volumeTolerance: Int? = null,
    val coveragePercentage: Float = 0f,
    val preferredExerciseIds: String = "",
    val avoidedExerciseIds: String = "",
    val lastTrainedDate: Long? = null,
    val sessionCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

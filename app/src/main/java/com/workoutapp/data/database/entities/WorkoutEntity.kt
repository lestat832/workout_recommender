package com.workoutapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.workoutapp.domain.model.WorkoutStatus
import com.workoutapp.domain.model.WorkoutType
import java.util.Date

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey
    val id: String,
    val date: Date,
    val type: WorkoutType,
    val status: WorkoutStatus
)
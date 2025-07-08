package com.workoutapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_exercises")
data class UserExerciseEntity(
    @PrimaryKey
    val id: String,
    val exerciseId: String,
    val isActive: Boolean = true
)
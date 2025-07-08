package com.workoutapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.workoutapp.data.database.converters.SetListConverter
import com.workoutapp.domain.model.Set

@Entity(tableName = "workout_exercises")
@TypeConverters(SetListConverter::class)
data class WorkoutExerciseEntity(
    @PrimaryKey
    val id: String,
    val workoutId: String,
    val exerciseId: String,
    val sets: List<Set>
)
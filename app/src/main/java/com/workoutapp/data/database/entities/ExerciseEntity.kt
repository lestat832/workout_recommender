package com.workoutapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.workoutapp.data.database.converters.MuscleGroupListConverter
import com.workoutapp.data.database.converters.StringListConverter
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType

@Entity(tableName = "exercises")
@TypeConverters(StringListConverter::class, MuscleGroupListConverter::class)
data class ExerciseEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val muscleGroups: List<MuscleGroup>,
    val equipment: String,
    val category: WorkoutType,
    val imageUrl: String? = null,
    val instructions: List<String> = emptyList(),
    val isUserCreated: Boolean = false,
    val createdAt: Long? = null
)
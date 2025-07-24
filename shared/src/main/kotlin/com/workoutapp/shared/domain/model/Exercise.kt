package com.workoutapp.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val muscleGroups: List<MuscleGroup>,
    val equipment: String,
    val category: WorkoutType,
    val imageUrl: String? = null,
    val instructions: List<String> = emptyList()
)

@Serializable
enum class MuscleGroup {
    CHEST,
    SHOULDER,
    TRICEP,
    LEGS,
    BACK,
    BICEP,
    CORE
}

@Serializable
enum class WorkoutType {
    PUSH,
    PULL
}
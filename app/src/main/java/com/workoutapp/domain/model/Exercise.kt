package com.workoutapp.domain.model

data class Exercise(
    val id: String,
    val name: String,
    val muscleGroups: List<MuscleGroup>,
    val equipment: String,
    val category: WorkoutType,
    val imageUrl: String? = null,
    val instructions: List<String> = emptyList(),
    val isUserCreated: Boolean = false
) {
    companion object {
        fun generateCustomId(): String = "custom_${java.util.UUID.randomUUID()}"
    }
}

enum class MuscleGroup {
    CHEST,
    SHOULDER,
    TRICEP,
    LEGS,
    BACK,
    BICEP,
    CORE
}

enum class WorkoutType {
    PUSH,
    PULL
}
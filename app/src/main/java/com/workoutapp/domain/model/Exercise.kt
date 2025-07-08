package com.workoutapp.domain.model

data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: String,
    val category: WorkoutType,
    val imageUrl: String? = null,
    val instructions: List<String> = emptyList(),
    val difficulty: Difficulty = Difficulty.BEGINNER
)

enum class MuscleGroup {
    CHEST,
    SHOULDER,
    TRICEP,
    LEGS,
    BACK,
    BICEP
}

enum class WorkoutType {
    PUSH,
    PULL
}

enum class Difficulty {
    BEGINNER,
    INTERMEDIATE,
    EXPERT
}
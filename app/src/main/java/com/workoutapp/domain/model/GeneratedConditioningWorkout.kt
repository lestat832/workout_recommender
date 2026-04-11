package com.workoutapp.domain.model

data class GeneratedConditioningWorkout(
    val format: WorkoutFormat,
    val exercises: List<Exercise>,
    val durationMinutes: Int
)

package com.workoutapp.domain.model

data class GeneratedWorkout(
    val type: WorkoutType,
    val exercises: List<Exercise>,
    val blockState: com.workoutapp.domain.usecase.BlockPeriodization.State? = null
)

package com.workoutapp.domain.model

enum class GymWorkoutStyle {
    STRENGTH,
    CONDITIONING;

    val appliesMonthlyDedup: Boolean
        get() = this == CONDITIONING
}

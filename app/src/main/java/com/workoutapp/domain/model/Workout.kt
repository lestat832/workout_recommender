package com.workoutapp.domain.model

import java.util.Date

data class Workout(
    val id: String,
    val date: Date,
    val type: WorkoutType,
    val status: WorkoutStatus,
    val gymId: Long? = null,
    val format: WorkoutFormat = WorkoutFormat.STRENGTH,
    val durationMinutes: Int? = null,
    val completedRounds: Int? = null,
    val exercises: List<WorkoutExercise> = emptyList()
)

enum class WorkoutStatus {
    IN_PROGRESS,
    COMPLETED,
    INCOMPLETE
}

data class WorkoutExercise(
    val id: String,
    val workoutId: String,
    val exercise: Exercise,
    val sets: List<Set>
)

data class Set(
    val reps: Int,
    val weight: Float,
    val completed: Boolean = false
)